from whiterabbit.clock import PocketWatch


def test_on_time():
    watch = PocketWatch(late_after_seconds=60)
    assert watch.check(45) == "on time"
    assert watch.is_late(45) is False


def test_late():
    watch = PocketWatch(late_after_seconds=60)
    assert watch.check(75) == "late!"
    assert watch.is_late(75) is True
