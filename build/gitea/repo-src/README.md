# white-rabbit

A tiny punctuality helper for Wonderland's scheduling service. "Oh dear! Oh dear!
I shall be late!" — so the White Rabbit keeps a pocket watch, and this package is it.

```python
from whiterabbit.clock import PocketWatch

watch = PocketWatch(late_after_seconds=60)
watch.check(45)   # -> "on time"
watch.check(75)   # -> "late!"
```

## Development

```bash
python3 -m venv venv && . venv/bin/activate
pytest
```

CI runs on Wonderland's Jenkins from the `Jenkinsfile` in this repository on every
branch and pull request.
