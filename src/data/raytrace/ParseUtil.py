import numpy as np
from typing import Any, Optional, List


class ParseState:
    """
    Mimics Java mutable state (str, op, b, d, StringBuilder).
    """
    def __init__(self):
        self.text: Optional[str] = None
        self.op: Any = None
        self.b: bool = False
        self.d: float = float("nan")


class ParseUtil:
    """
    Python translation of Java ParseUtil.
    Provides type-safe conversion layer between XML/string input
    and internal raytracing data types.
    """

    def __init__(self):
        self.state = ParseState()
        self._str_builder: List[str] = []

    # ---------------------------------------------------------------------
    # Basic scalar parsing
    # ---------------------------------------------------------------------

    @staticmethod
    def parse_string(o: Any) -> Optional[str]:
        if o is None or o == "":
            return None
        if isinstance(o, str):
            return o
        raise TypeError(f"parse_string: unsupported type {type(o)}")

    @staticmethod
    def parse_boolean(o: Any) -> bool:
        if isinstance(o, str):
            return o.lower() in ("true", "1", "yes")
        if isinstance(o, bool):
            return o
        raise TypeError(f"parse_boolean: unsupported type {type(o)}")

    @staticmethod
    def parse_integer(o: Any) -> int:
        if isinstance(o, str):
            return int(o)
        if isinstance(o, int):
            return o
        raise TypeError(f"parse_integer: unsupported type {type(o)}")

    @staticmethod
    def parse_double(o: Any, variables: dict, default=None) -> float:
        if isinstance(o, str):
            expr = o.strip()

            # evaluate expression with variables + numpy
            safe_globals = {
                "__builtins__": {},
                "np": np,
            }

            # optionally expose math functions directly
            safe_globals.update({
                "sin": np.sin,
                "cos": np.cos,
                "tan": np.tan,
                "sqrt": np.sqrt,
                "pi": np.pi,
            })

            try:
                result = eval(expr, safe_globals, variables)
            except NameError:
                result = default

            return float(result)

        if isinstance(o, (int, float)):
            return float(o)

        raise TypeError(f"parse_double: unsupported type {type(o)}")
    # ---------------------------------------------------------------------
    # Vector parsing (FIX for "{0,0,0}" issue)
    # ---------------------------------------------------------------------

    def parse_vector3(self, o: Any, variables: dict, default=None) -> np.ndarray:
        """
        Accepts:
          "{0,0,0}"
          "[0,0,0]"
          "(0,0,0)"
          "AnteriorCornea_pos+{92,0,0}"
          numpy arrays
          lists/tuples
        """

        if isinstance(o, str):
            expr = o.strip()

            # normalize vector syntax to Python lists
            expr = (
                expr.replace("{", "np.asarray([")
                .replace("}", "])")
            )

            # evaluate expression using variables

            try:
                result = eval(expr, {"np": np,
                                     "cos":np.cos,
                                     "sin":np.sin,
                                     "sqrt":np.sqrt} | variables)
            except NameError:
                result = default
            result = np.asarray(result, dtype=float)
            assert result.shape == (3,), f"Expected 3D vector, got shape {result.shape}"
            return result

        if isinstance(o, (list, tuple, np.ndarray)):
            return np.asarray(o, dtype=float)

        raise TypeError(f"parse_vector3: unsupported type {type(o)}")


    def parse_integer(self, o: Any, variables: dict, default=None) -> int:
        if isinstance(o, str):
            expr = o.strip()
            vars = {"np": np, "undef": np.nan, 'NaN': np.nan} | variables
            try:
                result = eval(expr, vars)
            except NameError:
                result = default
            return result
        if isinstance(o, int):
            return o
        raise TypeError(f"parse_integer: unsupported type {type(o)}")
    # ---------------------------------------------------------------------
    # Color parsing
    # ---------------------------------------------------------------------

    def parse_color(self, o: Any) -> np.ndarray:
        if isinstance(o, str):
            cleaned = o.strip("{}()[]").strip()
            parts = cleaned.split(",")
            return np.array([int(float(p)) for p in parts], dtype=int)

        if isinstance(o, (list, tuple, np.ndarray)):
            return np.asarray(o, dtype=int)

        raise TypeError(f"parse_color: unsupported type {type(o)}")

    # ---------------------------------------------------------------------
    # Matrix parsing (placeholder hook for your math engine)
    # ---------------------------------------------------------------------

    def parse_matrix(self, o: Any, variables:dict) -> np.ndarray:
        if isinstance(o, np.ndarray):
            return o

        if isinstance(o, list):
            return np.asarray(o, dtype=float)

        if isinstance(o, str):
            expr = (
                o.replace("{", "np.asarray([")
                .replace("}", "])")
            )

            # evaluate expression using variables

            result = eval(expr, {"np": np,
                                 "cos": np.cos,
                                 "sin": np.sin,
                                 "sqrt": np.sqrt} | variables)
            result = np.asarray(result, dtype=float)
            return result

        raise TypeError(f"parse_matrix: unsupported type {type(o)}")

    # ---------------------------------------------------------------------
    # File parsing
    # ---------------------------------------------------------------------

    def parse_file(self, o: Any):
        if o is None or o == "":
            return None

        if isinstance(o, str):
            return o  # or pathlib.Path(o)

        raise TypeError(f"parse_file: unsupported type {type(o)}")

    # ---------------------------------------------------------------------
    # Operation parsing (placeholder for your math system)
    # ---------------------------------------------------------------------

    def parse_operation(self, o: Any):
        """
        Replace with your OperationCompiler equivalent.
        """
        if isinstance(o, str):
            return o  # compile hook goes here

        if isinstance(o, (int, float)):
            return float(o)

        raise TypeError(f"parse_operation: unsupported type {type(o)}")

    # ---------------------------------------------------------------------
    # Position parsing (specialized vector + operation mix)
    # ---------------------------------------------------------------------

    def parse_position(self, o: Any) -> np.ndarray:
        return self.parse_vector3(o)

    # ---------------------------------------------------------------------
    # Reset state (Java equivalent)
    # ---------------------------------------------------------------------

    def reset(self):
        self.state = ParseState()
        self._str_builder.clear()