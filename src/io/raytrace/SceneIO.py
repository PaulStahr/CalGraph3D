import xml.etree.ElementTree as ET
import logging
from io import IOBase

from calgraph3d.data.raytrace import RaytraceScene

logger = logging.getLogger(__name__)
version = 2

def read_xml_values(elem):
    from calgraph3d.data.raytrace.OpticalObject import SceneObjectColumnType
    ct_list = []
    value_list = []
    for attr in elem.attrib:
        ct = SceneObjectColumnType.getByName(attr)
        if ct:
            ct_list.append(ct)
            value_list.append(elem.attrib[attr])
    return ct_list, value_list

def load_scene(in_stream: IOBase, scene:RaytraceScene, gui):
    import calgraph3d.data.raytrace.VolumePipeline as VolumePipeline
    from calgraph3d.data.raytrace.OpticalObject import SceneObjectColumnType
    from calgraph3d.data.raytrace.GuiOpticalSurfaceObject import GuiOpticalSurfaceObject
    from calgraph3d.data.raytrace.GuiOpticalVolumeObject import GuiOpticalVolumeObject
    from calgraph3d.data.raytrace.GuiTextureObject import GuiTextureObject
    from calgraph3d.data.raytrace import MeshObject
    from calgraph3d.data.raytrace.SpatialUnit import SpatialUnit
    from calgraph3d.data.raytrace.TextureMapping import get_by_name as get_texture_mapping
    from calgraph3d.data.raytrace.ParseUtil import ParseUtil

    tree = ET.parse(in_stream)
    root = tree.getroot()
    version_str = root.attrib.get("version", "-1")
    version_num = int(version_str)
    activate_auto_update = []
    parser = ParseUtil()
    for elem in root:
        tag = elem.tag
        ct_list, value_list = read_xml_values(elem)
        if tag in ("row", "surface"):
            scene.add(GuiOpticalSurfaceObject(content=(dict(zip(ct_list, value_list))), variables=scene.vs, parser=parser))
        elif tag == "volume":
            scene.add(GuiOpticalVolumeObject(content=(dict(zip(ct_list, value_list))), variables=scene.vs, parser=parser))
        elif tag == "texture":
            if version_num < 0:
                idx = ct_list.index(SceneObjectColumnType.PATH)
                value_list[idx] = f'"{value_list[idx]}"'
            scene.add(GuiTextureObject(content=(dict(zip(ct_list, value_list))), variables=scene.vs, parser=parser))
        elif tag == "mesh":
            scene.add(MeshObject(ct_list, value_list, scene.vs))
        elif tag == "Raybounds":
            scene.set_force_startpoint(elem.attrib.get("Start") or elem.attrib.get("Begin"))
            scene.set_force_endpoint(elem.attrib.get("End"))
        elif tag == "Environment":
            for k, v in elem.attrib.items():
                try:
                    if k == "Read" or k == "Write":
                        scene.set_environment_texture(v)
                    elif k == "RenderToTexture":
                        scene.set_render_to_texture(v)
                    elif k == "VerifyRefractionIndex":
                        scene.set_verify_refraction_indices(v.lower() == "true")
                    elif k == "Mapping":
                        scene.set_texture_mapping(get_texture_mapping(v))
                    else:
                        logger.warning(f"Unknown option {k}")
                except Exception as e:
                    logger.error(f"Can't set property {k} -> {v}", exc_info=e)
        elif tag == "Tool":
            if gui is not None:
                gui.panelTools.add(InterfacePanelFactory.get_instance(elem.text, scene.vs))
        elif tag == "Pipeline":
            if gui is not None:
                pipeline = gui.volumePipelines.add_pipeline().pipeline
                for child in elem:
                    if child.tag == "Generate":
                        pipeline.steps.append(VolumePipeline.GenerationCalculationStep(child.attrib["Bounds"]))
                    elif child.tag == "Calculate":
                        pipeline.steps.append(
                            VolumePipeline.CalculationCalcuationStep(
                                child.attrib["Ior"],
                                child.attrib["Translucency"],
                                child.attrib["EqValue"],
                                child.attrib["EqGiven"]
                            )
                        )
                if version_num <= 1 and isinstance(pipeline.steps[-1], VolumePipeline.CalculationCalcuationStep):
                    pipeline.steps[-1].ior = f'({pipeline.steps[-1].ior})/0x10000'
                for k, v in elem.attrib.items():
                    if k == "Volume":
                        pipeline.ovo = scene.get_volume_object(v)
                    elif k == "AutoUpdate" and v.lower() == "true":
                        activate_auto_update.append(pipeline)
                    elif k == "CalculateAtStartup":
                        pipeline.calcuteAtCreation = v.lower() == "true"
                pipeline.update_state()
        elif tag == "Unit":
            scene.spatialUnit = SpatialUnit.get_by_name(elem.text)
        elif tag == "Author":
            scene.author = elem.text
        elif tag == "Epsilon":
            scene.epsilon = float(elem.text)
        elif tag == "Description":
            if gui is not None:
                gui.textAreaProjectInformation.setText(elem.text)
        elif tag == "Variables":
            for child in elem:
                scene.vs[child.tag] = child.text
        elif tag == "Gui":
            try:
                from geometry.Geometry import parse as parse_geometry
                for k, v in elem.attrib.items():
                    if k == "Position":
                        parse_geometry(v, gui.paintOffset)
                    elif k == "Scale":
                        gui.panelVisualization.scale = float(v)
            except Exception as pe:
                logger.error(f"Can't parse attribute {k}", exc_info=pe)
        else:
            logger.warning(f"Unknown file entry {tag}")
    if version_num < 1:
        for volume in scene.volumeObjectList:
            gov = volume
            try:
                value = TransposeOperation(compile_op(gov.transformationStr)).calculate(scene.vs, controller)
                gov.set_value(SceneObjectColumnType.TRANSFORMATION, value, scene.vs, parser)
            except OperationParseException:
                try:
                    fallback = f"T({gov.transformationStr})"
                    gov.set_value(SceneObjectColumnType.TRANSFORMATION, fallback, scene.vs, parser)
                except OperationParseException:
                    logger.error("Can't load file in fallback mode")
    for surface in scene.optical_surface_objects:
        for column_type in SceneObjectColumnType.MINRADIUS, SceneObjectColumnType.MAXRADIUS:
            surface.updateValue(column_type, scene.vs, parser)

    scene.update_scene()

    for vp in scene.get_volume_pipelines():
        if vp.calcuteAtCreation:
            vp.update_variable_ids()
            vp.run()
    for vp in activate_auto_update:
        vp.set_auto_update(True)
    return scene

def save_scene(output_stream, only_selected, scene, gui):
    version = 2
    root = ET.Element("scene")
    root.set("version", str(version))

    def write_xml_values(oso, elem):
        types = oso.getTypes()
        for j in range(types.colSize()):
            ct = types.getCol(j)
            value = str(oso.getValue(ct))
            elem.set(ct.name, value)

    def write_xml_list(lst, table, elem_name):
        for i, item in enumerate(lst):
            if not only_selected or table.isRowSelected(i):
                elem = ET.Element(elem_name)
                write_xml_values(item, elem)
                root.append(elem)

    write_xml_list(scene.surfaceObjectList, gui.tableSurfaces, "surface")
    write_xml_list(scene.volumeObjectList, gui.tableVolumes, "volume")
    write_xml_list(scene.textureObjectList, gui.tableTextures, "texture")
    write_xml_list(scene.meshObjectList, gui.tableMeshes, "mesh")

    if not only_selected:
        raybounds = ET.Element("Raybounds")
        raybounds.set("Start", scene.getForceStartpointStr())
        raybounds.set("End", scene.getForceEndpointStr())
        root.append(raybounds)

        desc = ET.Element("Description")
        desc.text = gui.textAreaProjectInformation.getText()
        root.append(desc)

        env = ET.Element("Environment")
        env.set("Read", scene.environmentTextureString or "")
        env.set("Write", scene.writableEnvironmentTextureString or "")
        env.set("RenderToTexture", scene.renderToTextureString or "")
        env.set("VerifyRefractionIndex", str(scene.isVerifyRefractionIndexActivated()))
        env.set("Mapping", scene.environment_mapping.name)
        root.append(env)

        for i in range(gui.panelTools.getComponentCount()):
            tool_elem = ET.Element("Tool")
            comp = gui.panelTools.getComponent(i)
            if hasattr(comp, 'getContent'):
                tool_elem.text = comp.getContent()
            root.append(tool_elem)

        for vp_panel in gui.volumePipelines.getPipelines():
            pipeline = vp_panel.pipeline
            pipe_elem = ET.Element("Pipeline")
            for step in pipeline.steps:
                if isinstance(step, type(pipeline.GenerationCalculationStep)):
                    gen_elem = ET.Element("Generate")
                    gen_elem.set("Bounds", step.size)
                    pipe_elem.append(gen_elem)
                elif isinstance(step, type(pipeline.CalculationCalcuationStep)):
                    calc_elem = ET.Element("Calculate")
                    calc_elem.set("Ior", step.ior)
                    calc_elem.set("Translucency", step.translucency)
                    calc_elem.set("EqValue", step.givenValues)
                    calc_elem.set("EqGiven", step.isGiven)
                    pipe_elem.append(calc_elem)

            pipe_elem.set("AutoUpdate", str(pipeline.getAutoUpdate()))
            pipe_elem.set("CalculateAtStartup", str(pipeline.calcuteAtCreation))
            if pipeline.ovo:
                pipe_elem.set("Volume", pipeline.ovo.getId())
            root.append(pipe_elem)

        if scene.spatialUnit:
            unit_elem = ET.Element("Unit")
            unit_elem.text = scene.spatialUnit.name
            root.append(unit_elem)

        root.append(ET.Element("Author", text=scene.author))

        epsilon_elem = ET.Element("Epsilon")
        epsilon_elem.text = str(scene.epsilon)
        root.append(epsilon_elem)

        vars_elem = ET.Element("Variables")
        for i in range(scene.vs.sizeLocal()):
            var = scene.vs.get(i)
            var_elem = ET.Element(var.nameObject.string)
            var_elem.text = var.stringValue()
            vars_elem.append(var_elem)
        root.append(vars_elem)

        gui_elem = ET.Element("Gui")
        gui_elem.set("Position", str(gui.paintOffset))
        gui_elem.set("Scale", str(gui.panelVisualization.scale))
        root.append(gui_elem)

    tree = ET.ElementTree(root)
    tree.write(output_stream, encoding='unicode', xml_declaration=True, method='xml')