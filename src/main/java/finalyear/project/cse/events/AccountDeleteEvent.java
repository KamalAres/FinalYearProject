package finalyear.project.cse.events;

import finalyear.project.cse.auth.YouTubeAccount;

public class AccountDeleteEvent {

    private final YouTubeAccount account;

    public AccountDeleteEvent(final YouTubeAccount account) {
        this.account = account;
    }

    public YouTubeAccount getAccount() {
        return account;
    }

}
