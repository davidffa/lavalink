# davidffa/lavalink

A custom lavalink forked from [melike2d](https://github.com/melike2d/lavalink).

## Changes
- Support audio receiving (see how to use it [here](https://github.com/davidffa/lavalink/pull/2))
- Use [my custom lavaplayer-fork](https://github.com/davidffa/lavaplayer-fork/tree/custom) forked from [Walkyst's fork](https://github.com/walkyst/lavaplayer-fork).
- Add Getyarn, Reddit, Rumble, Yandex Music, Odysee, Tiktok sources. Yandex & Odysee search (`ymsearch:` `odsearch:` prefixes).
- Added WebSocket op code "ping" (responds with `{ "op": "pong" }`, useful to check WS latency between lavalink node and the bot). If you send the guildId property, lavalink responds with `{ "op": "pong", "ping": x }` where `x` is the latency between discord voice gateway and the lavalink node.
- Added GET /versions route, returns info about jvm version, kotlin version, spring version, build time, etc.
- Converted all stuff to kotlin
- Refactorings & project cleanup
- Dependency updates

## Credits

- [Freya Arbjerg](https://github.com/freyacodes) (For original lavalink)
- [melike2d](https://github.com/melike2d) (For custom lavalink)
- [sedmelluq](https://github.com/sedmelluq) (For original lavaplayer)
- [Walkyst](https://github.com/walkyst) (For lavaplayer-fork)
- [natanbc](https://github.com/natanbc) (For lavadsp audio filters)
