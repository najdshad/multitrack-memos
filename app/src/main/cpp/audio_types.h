#pragma once

#include <cstdint>

namespace sketch {

enum class TransportCommand : std::uint8_t {
    Arm = 0,
    Record,
    Stop,
    Play,
    Pause,
    Seek,
};

enum class RoutingMode : std::uint8_t {
    BuiltInMic = 0,
    BluetoothPlayback,
    WiredPlayback,
};

enum class EngineEventType : std::uint8_t {
    TransportChanged = 0,
    RouteChanged,
    RecordingSaved,
    PlaybackStarted,
    Error,
};

struct EngineCommand {
    TransportCommand type;
    std::int32_t track_index;
    std::int64_t position_ms;
};

struct EngineEvent {
    EngineEventType type;
    RoutingMode route;
    std::int64_t position_ms;
};

}  // namespace sketch
