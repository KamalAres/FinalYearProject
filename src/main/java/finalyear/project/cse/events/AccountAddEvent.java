package finalyear.project.cse.events;

import finalyear.project.cse.auth.YouTubeAccount;

public class AccountAddEvent {

    private final YouTubeAccount account;

    public AccountAddEvent(final YouTubeAccount account) {
        this.account = account;
    }

    public YouTubeAccount getAccount() {
        return account;
    }

}
