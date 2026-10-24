package com.alexai.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.alexai.assistant.commands.Action;
import com.alexai.assistant.commands.CommandParser;

import org.junit.Test;

public class CommandParserTest {
    @Test public void openYouTubeVariants() {
        assertEquals("YOUTUBE_OPEN", CommandParser.parse("Hey Alex, open YouTube.").id);
        assertEquals("YOUTUBE_OPEN", CommandParser.parse("launch youtube").id);
        assertEquals("YOUTUBE_OPEN", CommandParser.parse("go to YouTube for me").id);
    }

    @Test public void youtubeSearchKeepsOriginalCase() {
        Action a = CommandParser.parse("Alex, search YouTube for Java programming tutorials.");
        assertEquals("YOUTUBE_SEARCH", a.id);
        assertEquals("Java programming tutorials", a.param("query"));
    }

    @Test public void alarmAndTimer() {
        Action a = CommandParser.parse("set an alarm for 7:30 pm");
        assertEquals("19", a.param("hour"));
        assertEquals("30", a.param("minute"));
        assertEquals("0", CommandParser.parse("set an alarm for 12 am").param("hour"));
        assertEquals("300", CommandParser.parse("set a timer for 5 minutes").param("seconds"));
    }

    @Test public void settingsAndUtilities() {
        assertEquals("bluetooth", CommandParser.parse("open bluetooth settings").param("panel"));
        assertEquals("wifi", CommandParser.parse("show wi-fi settings").param("panel"));
        assertEquals("calculator", CommandParser.parse("open the calculator").param("target"));
        assertEquals("on", CommandParser.parse("turn on the flashlight").param("state"));
    }

    @Test public void mediaAndVolume() {
        assertEquals("pause", CommandParser.parse("pause the music").param("key"));
        assertEquals("next", CommandParser.parse("skip").param("key"));
        assertEquals("up", CommandParser.parse("increase media volume").param("direction"));
        assertEquals("down", CommandParser.parse("lower volume").param("direction"));
        assertEquals("mute", CommandParser.parse("mute media").param("direction"));
    }

    @Test public void appsAndFallthrough() {
        assertEquals("telegram", CommandParser.parse("open Telegram").param("app").toLowerCase());
        assertNull(CommandParser.parse("open my gallery and show my latest pictures"));
        assertNull(CommandParser.parse("tell me a joke"));
    }

    @Test public void smsKeepsMessageText() {
        Action a = CommandParser.parse("text +8801700000000 saying Hello There");
        assertEquals("SMS_DRAFT", a.id);
        assertEquals("Hello There", a.param("message"));
    }

    @Test public void v2Commands() {
        assertEquals("NAV_BACK", CommandParser.parse("go back").id);
        assertEquals("NAV_RECENTS", CommandParser.parse("show recents").id);
        assertEquals("down", CommandParser.parse("scroll down").param("direction"));
        assertEquals("image", CommandParser.parse("show a photo").param("kind"));
        assertEquals("video", CommandParser.parse("play a video").param("kind"));
        assertEquals("audio", CommandParser.parse("open music player").param("kind"));
        assertEquals("youtube", CommandParser.parse("close youtube").param("app"));
        assertEquals("UNINSTALL_APP", CommandParser.parse("uninstall Facebook").id);
        assertEquals("accessibility", CommandParser.parse("open accessibility settings").param("panel"));
    }

    @Test public void whatsappKeepsNumberAndMessage() {
        Action a = CommandParser.parse("send whatsapp to +8801700000000 saying Hi there");
        assertEquals("WHATSAPP_CHAT", a.id);
        assertEquals("+8801700000000", a.param("number"));
        assertEquals("Hi there", a.param("message"));
    }
}
