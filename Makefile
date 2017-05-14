PACKAGE = version3_networkLoop/
.SUFFIXES: .java .class
.java.class:
	javac $*.java

SRCS = Chatterbox.java \
	Conversation.java \
	Message.java \
	Network.java \
	User.java

all: classes Locator

classes: $(SRCS:.java=.class)

Locator: ListenerLocator.class

clean:
	$(RM) *.class
