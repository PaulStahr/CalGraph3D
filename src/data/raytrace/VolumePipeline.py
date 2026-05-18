import logging
import threading
import weakref
from dataclasses import dataclass
from typing import Callable, List, Optional


logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Placeholder implementations for external dependencies
# ---------------------------------------------------------------------------

class Volume:
    EMPTY_VOLUME_ARRAY = []

    def __init__(self, x=0, y=0, z=0):
        self.size = (x, y, z)

    def read_or_clone(self, other: "Volume"):
        self.size = other.size
        return self


class SortedIntegerArrayList(list):
    EMPTY_SORTED_INTEGER_ARRAY_LIST_ARRAY = []

    def clear(self):
        super().clear()


class Controller:
    pass


class Operation:
    pass


class OperationCompiler:
    @staticmethod
    def compile(expression, *_):
        return expression


class OperationCalculate:
    @staticmethod
    def get_variables(expression, target):
        # Placeholder variable extraction
        pass

    @staticmethod
    def to_int_array(value):
        return list(map(int, value))


class DataHandler:
    class runnableRunner:
        @staticmethod
        def run(runnable, _background=False):
            thread = threading.Thread(target=runnable.run, daemon=True)
            thread.start()

    class timedUpdater:
        @staticmethod
        def add(updater):
            pass

        @staticmethod
        def remove(updater):
            pass


# ---------------------------------------------------------------------------
# Calculation step definitions
# ---------------------------------------------------------------------------

class CalculationStep:
    pass


@dataclass
class GenerationCalculationStep(CalculationStep):
    size: str


@dataclass
class CalculationCalculationStep(CalculationStep):
    ior: str
    translucency: str
    given_values: str
    is_given: str


# ---------------------------------------------------------------------------
# Main pipeline
# ---------------------------------------------------------------------------

class VolumePipeline:
    def __init__(self, scene):
        self.cached_steps: List[Optional[Volume]] = []
        self.update_listener: List[weakref.ReferenceType] = []
        self.steps: List[CalculationStep] = []

        self.calculating = False
        self.ovo = None
        self.scene = scene

        self.calculate_at_creation = False
        self.auto_update = False

        self.begin = 0
        self.v_ids = []

        self._condition = threading.Condition()

        self.updater = self.VolumePipelineTimedUpdater(self)
        self.runnable = self.VolumeRunnable(self)

    # -----------------------------------------------------------------------
    # Timed updater
    # -----------------------------------------------------------------------

    class VolumePipelineTimedUpdater:
        def __init__(self, pipeline: "VolumePipeline"):
            self.pipeline = pipeline
            self.scene = pipeline.scene

            self.observer = self.scene.vs.create_variable_observer()
            self.all_changed_variables = self.observer.get_pendent_variable_list()

            self.mod_count = self.scene.vs.mod_count()

        def update(self):
            pipeline = self.pipeline

            self.scene.ray_update_handler.update()

            if self.scene.vs.mod_count() != self.mod_count:
                self.mod_count = self.scene.vs.mod_count()

                self.observer.update_changes()

                for i in range(len(pipeline.v_ids)):
                    if self.all_changed_variables.has_match(
                        pipeline.v_ids[i]
                    ):
                        with pipeline._condition:
                            pipeline.begin = min(i, pipeline.begin)

                            if not pipeline.calculating:
                                pipeline.calculating = True
                                DataHandler.runnableRunner.run(
                                    pipeline.runnable,
                                    False
                                )
                        break

        def get_update_interval(self):
            return 10

    # -----------------------------------------------------------------------
    # Runnable wrapper
    # -----------------------------------------------------------------------

    class VolumeRunnable:
        def __init__(self, pipeline: "VolumePipeline"):
            self.pipeline = pipeline

        def run(self):
            try:
                self.pipeline.pipe()
            except Exception:
                logger.exception("Exception while calculating volume")

    # -----------------------------------------------------------------------
    # Listener management
    # -----------------------------------------------------------------------

    def add_listener(self, runnable: Callable):
        self.update_listener = [
            ref for ref in self.update_listener
            if ref() is not None
        ]
        self.update_listener.append(weakref.ref(runnable))

    def remove_listener(self, runnable):
        self.update_listener = [
            ref for ref in self.update_listener
            if ref() is not runnable
        ]

    def update_state(self):
        dead = []

        for ref in self.update_listener:
            callback = ref()

            if callback is None:
                dead.append(ref)
            else:
                callback()

        for ref in dead:
            self.update_listener.remove(ref)

    # -----------------------------------------------------------------------
    # Variable dependency tracking
    # -----------------------------------------------------------------------

    def update_variable_ids(self):
        if len(self.v_ids) != len(self.steps):
            self.v_ids = [
                SortedIntegerArrayList()
                for _ in self.steps
            ]

        for v in self.v_ids:
            v.clear()

        for i, step in enumerate(self.steps):
            try:
                if isinstance(step, CalculationCalculationStep):
                    OperationCalculate.get_variables(
                        OperationCompiler.compile(step.given_values),
                        self.v_ids[i]
                    )

                    OperationCalculate.get_variables(
                        OperationCompiler.compile(step.ior),
                        self.v_ids[i]
                    )

                    OperationCalculate.get_variables(
                        OperationCompiler.compile(step.is_given),
                        self.v_ids[i]
                    )

                    OperationCalculate.get_variables(
                        OperationCompiler.compile(step.translucency),
                        self.v_ids[i]
                    )

                elif isinstance(step, GenerationCalculationStep):
                    OperationCalculate.get_variables(
                        OperationCompiler.compile(step.size),
                        self.v_ids[i]
                    )

            except Exception:
                pass

    # -----------------------------------------------------------------------
    # Core calculation pipeline
    # -----------------------------------------------------------------------

    def pipe(self):
        self.update_state()

        while len(self.cached_steps) < len(self.steps):
            self.cached_steps.append(Volume(0, 0, 0))

        try:
            while True:
                with self._condition:
                    current = self.begin
                    self.begin += 1

                    if current >= len(self.steps):
                        if self.cached_steps:
                            self.ovo.set_volume(
                                self.cached_steps[-1]
                            )

                        self.ovo.trigger_modification_events()

                        self.calculating = False
                        self._condition.notify_all()
                        break

                self.update_state()

                step = self.steps[current]

                if isinstance(step, CalculationCalculationStep):

                    if current != 0:
                        self.cached_steps[current] = (
                            self.cached_steps[current]
                            .read_or_clone(
                                self.cached_steps[current - 1]
                            )
                        )
                    else:
                        self.cached_steps[0].read_or_clone(
                            self.ovo.get_volume()
                        )

                    self.ovo.edit_values(
                        self.scene.copy_active_surfaces(),
                        OperationCompiler.compile(step.ior),
                        OperationCompiler.compile(step.translucency),
                        OperationCompiler.compile(step.given_values),
                        OperationCompiler.compile(step.is_given),
                        self.scene.vs,
                        self.cached_steps[current]
                    )

                elif isinstance(step, GenerationCalculationStep):

                    control = Controller()

                    values = OperationCalculate.to_int_array(
                        OperationCompiler
                        .compile(step.size)
                    )

                    self.cached_steps[current] = Volume(
                        values[0],
                        values[1],
                        values[2]
                    )

                    self.ovo.set_size(
                        values[0],
                        values[1],
                        values[2]
                    )

        except Exception:
            logger.exception("Can't pipe calculations")

            with self._condition:
                self.calculating = False
                self._condition.notify_all()

        self.update_state()

    # -----------------------------------------------------------------------
    # External API
    # -----------------------------------------------------------------------

    def run(self):
        with self._condition:
            self.begin = 0

            if not self.calculating:
                self.calculating = True

                DataHandler.runnableRunner.run(
                    self.runnable,
                    False
                )

    def set_auto_update(self, selected: bool):
        if self.auto_update != selected:

            if selected:
                DataHandler.timedUpdater.add(self.updater)
                self.update_variable_ids()
            else:
                DataHandler.timedUpdater.remove(self.updater)

            self.auto_update = selected

    def get_auto_update(self):
        return self.auto_update

    def is_calculating(self):
        return self.calculating

    def get_current_calculating_step(self):
        return self.begin - 1

    def block_on_calculation(self):
        self.updater.update()

        with self._condition:

            if self.calculating:
                logger.debug("block")

                self._condition.wait()

                logger.debug("unblock")
            else:
                logger.debug("skipblock")

            if self.calculating:
                raise RuntimeError(
                    "Thread should only wake at calculation finish"
                )