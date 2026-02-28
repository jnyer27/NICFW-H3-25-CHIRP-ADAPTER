"""
Tests for TIDRADIO TD-H3 nicFW V2.5 CHIRP driver.

Run with: python -m pytest tests/ -v

Requires CHIRP on PYTHONPATH. From CHIRP source tree:
  cd /path/to/chirp && python -m pytest /path/to/NICFW\ H3\ 25\ CHIRP\ ADAPTER/tests/ -v

Or add this repo root to PYTHONPATH and ensure chirp is installed.
"""

import sys
from pathlib import Path

import pytest

# Add repo root so we can import the standalone driver
REPO_ROOT = Path(__file__).resolve().parent.parent
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

try:
    from chirp import memmap
except ImportError:
    pytest.skip("CHIRP not available (add to PYTHONPATH)", allow_module_level=True)

# Import driver after path is set (driver itself imports chirp)
import tidradio_h3_nicfw25 as drv
from .build_sample_image import build_minimal_image


@pytest.fixture
def sample_image():
    """8 KB image with V2.5 magic and one channel (146.52 MHz)."""
    return memmap.MemoryMapBytes(build_minimal_image())


@pytest.fixture
def radio(sample_image):
    """Driver instance with sample image loaded (no serial)."""
    r = drv.TH3NicFw25(None)
    r._mmap = sample_image
    r.process_mmap()
    return r


def test_process_mmap(radio):
    """process_mmap parses MEM_FORMAT and sets _memobj."""
    assert radio._memobj is not None
    assert hasattr(radio._memobj, "memory")
    assert hasattr(radio._memobj, "settings")


def test_settings_magic(radio):
    """Settings block has V2.5 magic 0xD82F."""
    assert int(radio._memobj.settings.magic) == 0xD82F


def test_get_memory_channel_0(radio):
    """Channel 1 (first channel) is 146.52 MHz simplex from sample image."""
    mem = radio.get_memory(1)
    assert mem is not None
    assert not mem.empty
    assert mem.freq == 146520000  # Hz
    assert mem.duplex == ""
    assert mem.offset == 0
    assert "CH0" in mem.name or mem.name.strip() == "CH0"


def test_get_memory_empty_channel(radio):
    """Channel 2 is empty (zeros in sample image)."""
    mem = radio.get_memory(2)
    assert mem is not None
    assert mem.empty or mem.freq == 0


def test_get_settings(radio):
    """get_settings returns a group with at least squelch."""
    group = radio.get_settings()
    assert group is not None
    names = [e.get_name() for e in group if hasattr(e, "get_name")]
    assert "squelch" in names


def test_get_features(radio):
    """get_features returns memory_bounds and valid_bands."""
    rf = radio.get_features()
    assert rf.memory_bounds == (1, 198)
    assert len(rf.valid_bands) >= 1
    assert rf.valid_name_length == 12


def test_set_memory_roundtrip(radio):
    """set_memory then get_memory preserves data (in-memory, no upload)."""
    mem = radio.get_memory(1)
    mem.name = "TestCh"
    mem.freq = 446000000  # 446 MHz
    mem.duplex = ""
    mem.offset = 0
    radio.set_memory(mem)
    mem2 = radio.get_memory(1)
    assert mem2.freq == 446000000
    assert "TestCh" in mem2.name or mem2.name.strip() == "TestCh"


def test_load_from_image_file():
    """Load driver from tests/images sample image if present."""
    img_path = Path(__file__).resolve().parent / "images" / "TIDRADIO_TD-H3_nicFW25.img"
    if not img_path.exists():
        pytest.skip("Sample image not generated (run build_sample_image.py)")
    data = img_path.read_bytes()
    assert len(data) == 8192
    mmap = memmap.MemoryMapBytes(data)
    r = drv.TH3NicFw25(None)
    r._mmap = mmap
    r.process_mmap()
    mem = r.get_memory(1)
    assert mem is not None
    assert mem.freq == 146520000
    assert mem.number == 1
