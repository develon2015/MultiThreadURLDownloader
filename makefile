.PHONY:target
target: get_bin
	javac -d get_bin -sourcepath src -encoding utf-8 -target 8 -source 8 src/Main.java

get_bin:
	mkdir get_bin
