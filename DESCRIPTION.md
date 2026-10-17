<center>
  <h2>MobVitals</h2>
  <p><strong>Real-time packet-based health indicators, floating damage &amp; heal popups, and cinematic death corpse animations.</strong></p>
</center>

<hr>

<h3>1. Overview &amp; Purpose</h3>
<p>
  <strong>MobVitals</strong> transforms combat feedback and entity mortality across your Minecraft server. 
  Instead of entities abruptly vanishing into a vanilla puff of smoke, entities gracefully fall onto their side, persisting as interactive visual corpses with ambient particle wisps before dissolving into a thematic dissipation finale.
  Above their heads, sleek real-time health indicators and floating damage/heal popups inform players of exact combat vitals with zero server-side physical entity lag.
</p>

<h3>2. Key Features</h3>
<ul>
  <li><strong>100% Packet-Based Indicators:</strong> Healthbars and floating damage popups are rendered client-side via PacketEvents. No physical ArmorStands or entities are spawned in the server world.</li>
  <li><strong>Cinematic Corpse Physics:</strong> Fallen entities rotate into authentic corpse poses on the ground with zero collision or hitbox interference for players.</li>
  <li><strong>Per-Entity Floating Popups:</strong> Clean, uncluttered combat popups showing exact damage dealt or health recovered with randomized radial drift.</li>
  <li><strong>Modern Minimalist Healthbars:</strong> High-visibility segmented battery bars (<code>❙❙❙❙❙❙❙❙❙❙</code>) with dynamic Adventure MiniMessage color gradients based on remaining health.</li>
  <li><strong>Dense Area Optimization:</strong> Built-in crowd reduction algorithm dynamically accelerates corpse dissipation in high-density mob farms to prevent visual clutter.</li>
  <li><strong>Multi-Platform &amp; Folia Ready:</strong> Native support for Paper, Spigot, Purpur, and Folia region threading without race conditions or main-thread hitching.</li>
</ul>

<h3>3. Supported Platforms &amp; Minecraft Versions</h3>
<table>
  <tr>
    <th>Server Software</th>
    <th>Supported Versions</th>
    <th>Threading Architecture</th>
  </tr>
  <tr>
    <td>Paper / Purpur</td>
    <td>1.16.5 - 26.2+</td>
    <td>Synchronous / Async Task Chains</td>
  </tr>
  <tr>
    <td>Spigot</td>
    <td>1.16.5 - 26.2+</td>
    <td>Bukkit Scheduler</td>
  </tr>
  <tr>
    <td>Folia</td>
    <td>1.19.4 - 26.2+</td>
    <td>Region-Aware Entity Schedulers</td>
  </tr>
</table>

<h3>4. Requirements &amp; Dependencies</h3>
<ul>
  <li><strong>Java Runtime:</strong> Java 17, 21, or 25.</li>
  <li><strong>PacketEvents:</strong> v2.14.0 or higher installed in the <code>plugins/</code> folder.</li>
</ul>

<h3>5. Installation Guide</h3>
<ol>
  <li>Ensure your server runs Java 17 or newer.</li>
  <li>Download and install <code>packetevents-spigot-2.14.0.jar</code> into your <code>plugins/</code> directory.</li>
  <li>Place <code>MobVitals-1.0.0.jar</code> into your <code>plugins/</code> directory.</li>
  <li>Start or restart your server to generate default configurations.</li>
  <li>Customize styling and timings in <code>plugins/MobVitals/config.yml</code> as desired.</li>
</ol>

<h3>6. Commands &amp; Usage</h3>
<table>
  <tr>
    <th>Command</th>
    <th>Description</th>
    <th>Permission</th>
  </tr>
  <tr>
    <td><code>/mobvitals help</code></td>
    <td>Displays available plugin commands and syntax.</td>
    <td>None</td>
  </tr>
  <tr>
    <td><code>/mobvitals toggle</code></td>
    <td>Toggles personal visibility of health indicators on or off.</td>
    <td><code>mobvitals.use</code></td>
  </tr>
  <tr>
    <td><code>/mobvitals status</code></td>
    <td>Displays active indicators, corpses, and viewer counts.</td>
    <td><code>mobvitals.use</code></td>
  </tr>
  <tr>
    <td><code>/mobvitals test &lt;profile&gt;</code></td>
    <td>Previews a cinematic corpse death effect locally.</td>
    <td><code>mobvitals.admin</code></td>
  </tr>
  <tr>
    <td><code>/mobvitals reload</code></td>
    <td>Reloads configuration settings dynamically.</td>
    <td><code>mobvitals.admin</code></td>
  </tr>
</table>

<h3>7. Permissions</h3>
<ul>
  <li><code>mobvitals.use</code> &mdash; Allows viewing indicators, checking status, and using <code>/mobvitals toggle</code>. (Default: <code>true</code> for all players).</li>
  <li><code>mobvitals.admin</code> &mdash; Allows configuration reloads and death profile testing. (Default: <code>op</code>).</li>
</ul>

<h3>8. Configuration Overview</h3>
<p>The configuration file allows extensive customization of display distance, bar symbols, formats, blacklist, and corpse timings:</p>
<pre>
indicators:
  enabled: true
  view-distance: 24.0
  height-offset: 0.25
  show-only-when-damaged: true
  damaged-display-seconds: 4.0
  bar-length: 10
  symbol-full: "❙"
  symbol-empty: "❙"
  color-high: "&lt;green&gt;"
  color-medium: "&lt;yellow&gt;"
  color-low: "&lt;gold&gt;"
  color-critical: "&lt;red&gt;"
  color-empty: "&lt;dark_gray&gt;"
  format: "&lt;white&gt;&lt;bold&gt;&lt;name&gt;&lt;/bold&gt;&lt;/white&gt; &lt;bar&gt; &lt;color&gt;&lt;current&gt;&lt;reset&gt;&lt;dark_gray&gt;/&lt;/dark_gray&gt;&lt;gray&gt;&lt;max&gt;&lt;/gray&gt; &lt;red&gt;❤&lt;/red&gt;"

corpses:
  duration-seconds: 4.0
  enable-corpse-body: true
  enable-ambient-particles: true
  dense-corpse-reduction: true
  dense-radius: 6.0
</pre>

<h3>9. Performance &amp; Technical Safety</h3>
<ul>
  <li><strong>Zero Entity Footprint:</strong> MobVitals does not create or register Minecraft entities in chunk saves or server ticking loops. All markers exist solely in memory as network packets.</li>
  <li><strong>Thread-Safe Concurrency:</strong> Backed by concurrent maps and Folia region task schedulers, guaranteeing crash-free execution under extreme combat loads.</li>
  <li><strong>Adaptive Packet Dispatch:</strong> Visual packets are dispatched silently only to players within configured line-of-sight view distances.</li>
</ul>

<h3>10. Troubleshooting &amp; FAQ</h3>
<details>
  <summary><strong>Do dead mob bodies block arrows, attacks, or player movement?</strong></summary>
  <p>No. When an entity dies, its client-side health is zeroed out and its pose is set to dying. In vanilla Minecraft client logic, entities with zero health have zero collision and pass through all player crosshair clicks and projectiles.</p>
</details>
<details>
  <summary><strong>Can players disable health bars if they prefer clean vanilla visuals?</strong></summary>
  <p>Yes. Any player can execute <code>/mobvitals toggle</code> to instantaneously hide all packet-based indicators for their client.</p>
</details>
<details>
  <summary><strong>Does MobVitals work on modern Minecraft versions like 1.20, 1.21, and 26.2?</strong></summary>
  <p>Yes. MobVitals is tested and verified on modern Spigot, Paper, and Folia running Minecraft 1.16.5 up to 26.2+.</p>
</details>
