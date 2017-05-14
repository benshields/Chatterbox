Benjamin Shields
Distributed project - Chatterbox (P2P chat)

To execute...

to make all executables
	make all

to run the required helper server (which simply tells new users which port to connect to in order
to join the chat loop)
	java  version3_networkLoop.ListenerLocator

to create a new user which joins the chat
	java version3_networkLoop.Chatterbox

At this point you can keep adding users, and when you want to, close the helper server (the
listener locator) by typing
	"exit"
in the listener locators console. After you close the listener locator, you can no longer add
new users.

To chat, simply type the name of the single recipient, hit ENTER, then type the body of the
message, and hit ENTER again, like so:

ben+ENTER
Hello there! Glad to meet you!+ENTER

--------------------

I'm sorry I could not implement everything I wanted to, I will finish more over the summer.
May 3rd had too many deadlines with too short of windows to work. I didn't slack off or
waste any time, this is all I could accomplish with what I had to do.

So users cannot group chat, nor logout.