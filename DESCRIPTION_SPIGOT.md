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
