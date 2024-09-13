# Detect OS
ifeq ($(OS),Windows_NT)
    detected_OS := Windows
    RM = del /s /q
    CP_SEP = ;
else
    detected_OS := $(shell uname)
    RM = rm -rf
    CP_SEP = :
endif

JAVAC=javac
JAVA=java
BIN_DIR=../bin
LIB_DIR=../lib
JSON_JAR=$(LIB_DIR)/json-20240303.jar
MAIN_SERVER_PACKAGE=server/network
MAIN_SERVER_CLASS=GameServer
MAIN_CLIENT_PACKAGE=client
MAIN_CLIENT_CLASS=Client

.PHONY: all compile-server compile-client run-server run-client clean help

all: compile-server compile-client

compile-server:
	$(JAVAC) -cp ".${CP_SEP}$(JSON_JAR)${CP_SEP}src" -d $(BIN_DIR) $(MAIN_SERVER_PACKAGE)/$(MAIN_SERVER_CLASS).java

compile-client:
	$(JAVAC) -cp ".${CP_SEP}$(JSON_JAR)${CP_SEP}src" -d $(BIN_DIR) $(MAIN_CLIENT_PACKAGE)/$(MAIN_CLIENT_CLASS).java


run-server:
	$(JAVA) -cp "$(BIN_DIR)$(CP_SEP)$(JSON_JAR)$(CP_SEP)src" server.network.GameServer

run-client:
	$(JAVA) -cp "$(BIN_DIR)$(CP_SEP)$(JSON_JAR)$(CP_SEP)src" client.Client


# Clean command using the correct RM command
clean:
	$(RM) $(BIN_DIR)/*
	$(RM) *.class


help:
	@echo "Usage: make [command]"
	@echo "Available commands:"
	@echo "  all             Compile all source files"
	@echo "  compile-server  Compile the project's server"
	@echo "  compile-client  Compile the project's client"
	@echo "  run-server      Start the game server"
	@echo "  run-client      Start a game client"
	@echo "  clean           Remove compiled class files"
	@echo "  help            Display this help message"
