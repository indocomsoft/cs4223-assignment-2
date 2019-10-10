package coherence.devices

import coherence.bus.BusDelegate

trait Cache[Message]
    extends Device
    with BusDelegate[Message]
    with MemoryDelegate {}
