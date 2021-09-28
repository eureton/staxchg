# staxchg

A [Stack Exchange](https://stackexchange.com) client for the terminal.

## Installation

* Download the [latest release binaries](https://github.com/eureton/staxchg/releases/tag/v0.1.1).

## Configuration

The app can be used with no configuration. If you wish to override the default behavior, create a `$HOME/.staxchg.conf` file and type in your preferences in `KEY=VALUE` format. Supported keys are described below.

### `SITE`

The `StackExchange` site to connect to. Default value: `stackoverflow`.

Example:

  ```
  SITE=superuser
  ```

There is no need to restart the app for a change to `SITE` to take effect. The next query will search the specified site.

### `ACCESS_TOKEN`

For those who wish to query using their StackExchange user quote, follow the steps below:
* authorize the app:
  * follow [this link](https://stackoverflow.com/oauth/dialog?client_id=19510&scope=no_expiry&redirect_uri=https://stackoverflow.com/oauth/login_success) in your browser
  * authorize the app
  * after authorization, you will be redirected and `access_token` will be placed in the url hash
  * add the following line to your `$HOME/.staxchg.conf` file:
    ```
    ACCESS_TOKEN=<your-access-token>
    ```

## Code syntax highlighting

Code snippets are automatically highlighted if you have [`skylighting`](https://github.com/jgm/skylighting) in your `PATH`. To install it, check your package manager first (`.deb` and `.rpm` are available). Alternatively, follow [these instructions](https://github.com/jgm/skylighting#installing).

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

## License

[MIT License](https://github.com/eureton/staxchg/blob/master/LICENSE)
