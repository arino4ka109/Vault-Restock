## 🖤 Vault Restock

**Vault Restock** — an efficient plugin that allows players to repeatedly open Vaults in Trial Chambers with fully customizable cooldown times.

---

### 🛠 Commands and Permissions

All commands are available to operators (**OP**) or players with a special permission (**restockstorage.admin**).

| Command | Description | Permission |
| --- | --- | --- |
| `/restock` | Displays a message with the current cooldown status in the config and a list of commands. | `restockstorage.admin` |
| `/restock time <time>` | Changes the cooldown time and saves it to `config.yml`. | `restockstorage.admin` |
| `/restock storage` | Instantly refreshes the Vault that the player is currently looking at. | `restockstorage.admin` |

---

### ⏱ Supported Time Formats

When configuring the time via the `/restock time` command or the configuration file, the following suffixes can be used:

* `tick` — game ticks (**1 second = 20 ticks**)
* `sec` — seconds
* `min` — minutes
* `day` — days
* `week` — weeks
* `mon` — months (**30 days**)

> **Usage example:** `/restock time 45sec` or `/restock time 15tick`
