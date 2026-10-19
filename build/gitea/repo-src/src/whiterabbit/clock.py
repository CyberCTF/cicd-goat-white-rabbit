"""A pocket watch that tells Wonderland whether it is running late."""

from dataclasses import dataclass


@dataclass
class PocketWatch:
    """Reports lateness against a deadline measured in seconds."""

    late_after_seconds: int = 60

    def is_late(self, elapsed_seconds: int) -> bool:
        """Return True when ``elapsed_seconds`` has passed the deadline."""
        return elapsed_seconds > self.late_after_seconds

    def check(self, elapsed_seconds: int) -> str:
        """Return a human-readable status for the elapsed time."""
        return "late!" if self.is_late(elapsed_seconds) else "on time"
