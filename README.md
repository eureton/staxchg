# staxchg

A Stack Exchange client for the terminal.

## Installation

* Download from https://github.com/eureton/staxchg
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
