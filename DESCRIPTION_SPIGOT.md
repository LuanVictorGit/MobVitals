[CENTER]
[B][SIZE=6]MobVitals[/SIZE][/B]
[SIZE=4]Real-time packet-based health indicators, floating damage & heal popups, and cinematic corpse death animations.[/SIZE]
[SIZE=3]100% Virtual Entities via PacketEvents | Paper, Spigot, and Folia Supported[/SIZE]
[/CENTER]

[B][SIZE=5]✨ Overview[/SIZE][/B]
MobVitals enhances combat feedback and entity mortality across your Minecraft server. Instead of entities abruptly vanishing into a vanilla puff of smoke, entities gracefully fall onto their side, persisting as interactive visual corpses with ambient particle wisps before dissolving into a thematic dissipation finale.

Above their heads, sleek real-time health indicators and floating damage/heal popups inform players of exact combat vitals with [B]zero server-side physical entity lag[/B].

[B][SIZE=5]⚡ Key Features[/SIZE][/B]
[LIST]
[*] [B]100% Packet-Based Indicators:[/B] Healthbars and floating damage popups are rendered client-side via PacketEvents. No physical ArmorStands or entities are spawned in the server world.
[*] [B]Cinematic Corpse Physics:[/B] Fallen entities rotate into authentic corpse poses on the ground with zero collision or hitbox interference for players.
[*] [B]Per-Entity Floating Popups:[/B] Clean, uncluttered combat popups showing exact damage dealt or health recovered with randomized radial drift.
[*] [B]Modern Minimalist Healthbars:[/B] High-visibility segmented battery bars ([FONT=Courier New]❙❙❙❙❙❙❙❙❙❙[/FONT]) with dynamic Adventure MiniMessage color gradients based on remaining health.
[*] [B]Dense Area Optimization:[/B] Built-in crowd reduction algorithm dynamically accelerates corpse dissipation in high-density mob farms to prevent visual clutter.
[*] [B]Multi-Platform & Folia Ready:[/B] Native support for Paper, Spigot, Purpur, and Folia region threading without race conditions or main-thread hitching.
[/LIST]

[B][SIZE=5]🔧 Changelog v1.0.6[/SIZE][/B]
[LIST]
[*] [B]Popups Always Disappear:[/B] Fixed damage/heal popups staying frozen in the air forever when [FONT=Courier New]indicators.enabled[/FONT] (or the [FONT=Courier New]general.enabled[/FONT] master switch) was off. Popup drift and removal now run independently of health bars.
[*] [B]World Changes No Longer Freeze Popups:[/B] A player with an active health bar changing worlds (portal, teleport, RTP) threw an error every tick that froze every popup on the server while the bar was alive. The bar is now respawned in the new world.
[*] [B]Resilient Updates:[/B] One failing health bar or popup no longer stalls the others; the faulty entry is removed and logged.
[*] [B]Master Switch Respected:[/B] [FONT=Courier New]general.enabled: false[/FONT] now also turns popups off.
[/LIST]

[B][SIZE=5]🔧 Changelog v1.0.5[/SIZE][/B]
[LIST]
[*] [B]Smooth Death Fall & 2-Tick Anchor (~100ms - 250ms):[/B] Permitted natural death event packets (status 3) so entities gracefully tilt and lie flat onto the ground before anchoring at 2 ticks with a 5-tick (~250ms) stability re-confirmation directly at ambient particle coordinates.
[*] [B]Damage Tint Elimination:[/B] Cancelled damage and hurt animation packets on fallen entities so corpses retain their natural colors and textures throughout the entire duration.
[*] [B]Instant & Reliable Hit Holograms:[/B] Synchronously dispatched floating damage popups on every hit (including fatal hits and rapid consecutive attacks), guaranteeing that damage indicators never fail to appear.
[/LIST]

[B][SIZE=5]🔧 Changelog v1.0.4[/SIZE][/B]
[LIST]
[*] [B]1.21.2+ & 26.2 EntityPositionSync Teleportation:[/B] Implemented modern Minecraft 1.21.2+ [FONT=Courier New]ClientboundEntityPositionSyncPacket[/FONT] position syncing. Dead corpses now accurately teleport directly onto their particle spawn point 1 tick after death, completely resolving knockback separation.
[/LIST]

[B][SIZE=5]🔧 Changelog v1.0.3[/SIZE][/B]
[LIST]
[*] [B]Scoreboard Team Collision Isolation:[/B] Applied native scoreboard team rules to prevent player collision interference and fast-tracked ground positioning.
[/LIST]

[B][SIZE=5]🔧 Changelog v1.0.2[/SIZE][/B]
[LIST]
[*] [B]1-Tick Damage Synchronization:[/B] Delayed healthbar recalculation by 1 tick on damage/heal events to reflect Bukkit's exact post-damage health calculation.
[*] [B]Corpse Skin Normalization:[/B] Eliminated permanent red hurt overlay on dead mob bodies by cancelling status 3 packets and adopting packet-based sleep poses.
[*] [B]Corpse Collision Elimination:[/B] Added scoreboard team collision rules ([FONT=Courier New]CollisionRule.NEVER[/FONT]) to completely stop corpses from pushing players.
[*] [B]10-Tick Teleport Anchor:[/B] Anchors the corpse body at 10 ticks with zero delta movement, locking corpse coordinates and ambient particle wisps into vertical alignment.
[*] [B]Self-Indicator Suppression:[/B] Players can no longer view their own healthbars or popups directly on themselves.
[*] [B]Heal Tag Fix:[/B] Sanitized invalid [FONT=Courier New]<emerald>[/FONT] color tag in heal format.
[/LIST]

[B][SIZE=5]💻 Supported Platforms & Versions[/SIZE][/B]
[LIST]
[*] [B]Minecraft Versions:[/B] 1.16.5, 1.17.x, 1.18.x, 1.19.x, 1.20.x, 1.21.x, 26.2+
[*] [B]Server Software:[/B] Paper, Purpur, Spigot, Folia
[*] [B]Required Java:[/B] Java 17 or higher
[*] [B]Required Dependency:[/B] PacketEvents 2.14.0+
[/LIST]

[B][SIZE=5]⌨ Commands & Permissions[/SIZE][/B]
[TABLE]
[TR]
[TH]Command[/TH]
[TH]Description[/TH]
[TH]Permission[/TH]
[/TR]
[TR]
[TD][FONT=Courier New]/mobvitals help[/FONT][/TD]
[TD]Displays available plugin commands.[/TD]
[TD]None[/TD]
[/TR]
[TR]
[TD][FONT=Courier New]/mobvitals toggle[/FONT][/TD]
[TD]Toggles personal visibility of indicators.[/TD]
[TD][FONT=Courier New]mobvitals.use[/FONT][/TD]
[/TR]
[TR]
[TD][FONT=Courier New]/mobvitals status[/FONT][/TD]
[TD]Displays active indicator and corpse statistics.[/TD]
[TD][FONT=Courier New]mobvitals.use[/FONT][/TD]
[/TR]
[TR]
[TD][FONT=Courier New]/mobvitals test <profile>[/FONT][/TD]
[TD]Previews a cinematic corpse death effect.[/TD]
[TD][FONT=Courier New]mobvitals.admin[/FONT][/TD]
[/TR]
[TR]
[TD][FONT=Courier New]/mobvitals reload[/FONT][/TD]
[TD]Reloads configuration dynamically.[/TD]
[TD][FONT=Courier New]mobvitals.admin[/FONT][/TD]
[/TR]
[/TABLE]

[B][SIZE=5]⭐ Support & Reviews[/SIZE][/B]
If you find MobVitals valuable for your server community, please consider leaving a 5-star rating on SpigotMC! For bug reports or feature requests, join our community Discord.
