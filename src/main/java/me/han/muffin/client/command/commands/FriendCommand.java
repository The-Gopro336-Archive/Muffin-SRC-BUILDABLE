package me.han.muffin.client.command.commands;

import me.han.muffin.client.command.Argument;
import me.han.muffin.client.command.Command;
import me.han.muffin.client.friend.Friend;
import me.han.muffin.client.manager.managers.FriendManager;

public class FriendCommand extends Command{

/*
    public FriendCommand() {
        super(new String[]{"friend", "friends", "f"}, new Argument("username"));
    }


    @Override
    public String dispatch() {
        String username = getArgument("username").getValue();
        String alias = getArgument("username").getValue();

        Friend friend = Muffin.getInstance().getFriendManager().getFriendByAliasOrLabel(username);
        String oldAlias = friend.getAlias();

        if (Muffin.getInstance().getFriendManager().isFriend(username)) {
            Muffin.getInstance().getFriendManager().remove(friend);
            return "Removed friend with alias " + oldAlias + ".";
        }

        if (!Muffin.getInstance().getFriendManager().isFriend(username)) {
            Muffin.getInstance().getFriendManager().add(new Friend(username, alias));
            return "Added friend with alias " + alias + ".";
        }

        return "User not found.";
    }

     */



    public FriendCommand() {
        super(new String[]{"friend", "friends", "f"}, new Argument("add/remove/del/delete"), new Argument("username"));
    }


    @Override
    public String dispatch() {
        Argument actionArgument = getArgument("add/remove/del/delete");
        if (actionArgument == null) return "Invalid actions.";

        Argument usernameArgument = getArgument("username");
        if (usernameArgument == null) return "";

        String action = actionArgument.getValue();
        String usernameValue = usernameArgument.getValue();

        if (action.equalsIgnoreCase("add")) {
            if (FriendManager.isFriend(usernameValue)) return "That user is already a friend.";
            FriendManager.add(new Friend(usernameValue, usernameValue));
            return "Added friend with alias " + usernameValue + ".";
        }

        if (action.equalsIgnoreCase("remove") || action.equalsIgnoreCase("del") || action.equalsIgnoreCase("delete")) {
            if (!FriendManager.isFriend(usernameValue)) return "That user is not a friend.";

            Friend friend = FriendManager.getFriendByAliasOrLabel(usernameValue);
            if (friend == null) return "This is not your friend.";

            String oldAlias = friend.getAlias();
            FriendManager.remove(friend);
            return "Removed friend with alias " + oldAlias + ".";
        }

        return "Report to han.";
    }


}
