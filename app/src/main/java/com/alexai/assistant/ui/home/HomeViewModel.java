package com.alexai.assistant.ui.home;

import androidx.lifecycle.ViewModel;

import com.alexai.assistant.ai.ConversationManager;

import java.util.ArrayList;
import java.util.List;

/** Keeps the chat alive across rotation and theme changes. */
public class HomeViewModel extends ViewModel {
    public final List<Message> messages = new ArrayList<>();
    public final ConversationManager conversation = new ConversationManager();
    public boolean historyLoaded;
}
