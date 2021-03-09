# staxchg

A Stack Exchange client for the terminal.

## Installation

* Download the Linux amd64 standalone JAR from [here](https://github.com/eureton/staxchg/releases/download/v0.1.0/staxchg-0.1.0-standalone.jar).

## Configuration

* create a `$HOME/.staxchg.conf` file with the following contents:
  ```
  CLIENT_ID=19510
  API_KEY=pUdRXaEu0*w82Brq7xzlyw((
  SITE=stackoverflow
  ```
* authorize the app:
  * follow [this link](https://stackoverflow.com/oauth/dialog?client_id=19510&scope=no_expiry&redirect_uri=https://stackoverflow.com/oauth/login_success) in your browser
  * authorize the app
  * after authorization, you will be redirected and `access_token` will be placed in the url hash
  * add the following line to your `$HOME/.staxchg.conf` file:
    ```
    ACCESS_TOKEN=<your-access-token>
    ```

## Usage

`staxchg` uses VI keybindings where possible.

* there are two screens: `questions` and `answers`
* the app starts in the `questions` screen
* to search for questions, press `/`
* to scroll the list of fetched questions, press `J` / `K`
* to scroll the currently selected question
  * by a single line, press `j`/ `k`
  * by a half-screen, press `CTRL-d` / `CTRL-u`
  * by a full screen, press `Space` / `b`
* to view answers for the currently selected question, press `Enter`
* when viewing answers:
  * to move to the next answer, press `l`
  * to move to the previous answer, press `h`
  * to return to the questions screen, press `Backspace`

## Options

By default, `staxchg` searches StackOverflow. To search in a different StackExchange site, update the `SITE` parameter in your `.conf` file. For example, `SITE=superuser` searches [superuser.com](https://superuser.com), `SITE=serverfault` searches [serverfault.com](https://serverfault.com) and `SITE=unix` searches [unix.stackexchange.com](https://unix.stackexchange.com).

There is no need to restart the app for a change to `SITE` to take effect. The next query will fetch from the specified site.

## License

Copyright © 2020 Eureton OÜ

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
