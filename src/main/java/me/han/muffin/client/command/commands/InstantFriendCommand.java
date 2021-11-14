package me.han.muffin.client.command.commands;

import me.han.muffin.client.command.Argument;
import me.han.muffin.client.command.Command;
import me.han.muffin.client.friend.Friend;
import me.han.muffin.client.manager.managers.FriendManager;

public class InstantFriendCommand {

    public static final class Add extends Command {
        public Add() {
            super(new String[]{"add", "a"}, new Argument("username"));
        }

        @Override
        public String dispatch() {
            Argument usernameArgument = getArgument("username");
            if (usernameArgument == null) return "Invalid values.";

            String username = usernameArgument.getValue();

            if (FriendManager.isFriend(username)) return "That user is already a friend.";

            FriendManager.add(new Friend(username, username));
            return "Added friend with alias " + username + ".";
        }
    }

    public static final class Remove extends Command {
        public Remove() {
            super(new String[]{"remove", "rem"}, new Argument("username/alias"));
        }

        @Override
        public String dispatch() {
            Argument usernameArgument = getArgument("username/alias");
            if (usernameArgument == null) return "Invalid values.";

            String name = usernameArgument.getValue();
            if (!FriendManager.isFriend(name)) return "That user is not a friend.";

            Friend friend = FriendManager.getFriendByAliasOrLabel(name);
            if (friend == null) return "This user is not your friend";

            String oldAlias = friend.getAlias();
            FriendManager.remove(friend);
            return "Removed friend with alias " + oldAlias + ".";
        }
    }

}