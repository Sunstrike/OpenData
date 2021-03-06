package openeye.notes;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.EnumChatFormatting;
import openeye.logic.InjectedDataStore;
import openeye.logic.ModState;
import openeye.logic.StateHolder;
import openeye.notes.entries.*;
import openeye.responses.ResponseDangerousFile;
import openeye.responses.ResponseKnownCrash;
import openeye.responses.ResponseModMsg;
import openeye.storage.Storages;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class NoteCollector {

	private static final Comparator<NoteEntry> NOTE_COMPARATOR = new Comparator<NoteEntry>() {
		@Override
		public int compare(NoteEntry o1, NoteEntry o2) {
			int result = -Integer.compare(o1.level, o2.level);
			if (result != 0) return result;

			return o1.file.compareTo(o2.file);
		}
	};

	public static class ScreenNotification {
		private final int level;
		public final ChatMessageComponent msg;

		private ScreenNotification(int level, ChatMessageComponent msg) {
			this.level = level;
			this.msg = msg;
		}
	}

	private boolean important;

	public static final NoteCollector INSTANCE = new NoteCollector();

	private final List<NoteEntry> notes = Lists.newArrayList();

	private ScreenNotification menuLine;

	private NoteCategory maxCategory = NoteCategory.INFO;

	private NoteCollector() {}

	private void addMenuLine(int level, ChatMessageComponent msg) {
		if (menuLine == null || level > menuLine.level) menuLine = new ScreenNotification(level, msg);
	}

	public void addNote(NoteEntry entry) {
		notes.add(entry);
		maxCategory = Ordering.natural().max(maxCategory, entry.category);
		important |= entry.category.important;
	}

	public void addNote(File file, ResponseModMsg note) {
		NoteEntry entry = new MsgNoteEntry(file, note);
		addNote(entry);
	}

	public void addNote(File file, ResponseDangerousFile note) {
		addNote(new DangerousFileEntry(file, note));
		addMenuLine(64, ChatMessageComponent.createFromTranslationKey("openeye.main_screen.dangerous_file").setBold(true).setColor(EnumChatFormatting.RED));
	}

	public void addNote(ResponseKnownCrash note) {
		if (Strings.isNullOrEmpty(note.note)) {
			addNote(new ReportedCrashEntry(note));
			addMenuLine(8, ChatMessageComponent.createFromTranslationKey("openeye.main_screen.crash_reported"));
		} else {
			addNote(new ResolvedCrashEntry(note));
			addMenuLine(32, ChatMessageComponent.createFromTranslationKey("openeye.main_screen.known_crash").setColor(EnumChatFormatting.GREEN));
		}
	}

	public boolean isEmpty() {
		return notes.isEmpty();
	}

	public NoteCategory getMaxLevel() {
		return maxCategory;
	}

	public ScreenNotification getScreenMsg() {
		return menuLine;
	}

	public boolean hasImportantNotes() {
		return important;
	}

	public List<NoteEntry> getNotes() {
		Collections.sort(notes, NOTE_COMPARATOR);
		return ImmutableList.copyOf(notes);
	}

	private void addIntroNote(int id, String url) {
		String title = "openeye.note.title.intro" + id;
		String content = "openeye.note.content.intro" + id;
		addNote(new SystemNoteEntry(NoteLevels.SYSTEM_NOTIFICATION_LEVEL + 16 - id,
				ChatMessageComponent.createFromTranslationKey(title),
				ChatMessageComponent.createFromTranslationKey(content),
				url));
	}

	public void finishNoteCollection() {
		ModState state = StateHolder.state();

		if (!state.infoNotesDisplayed) {
			File reportDir = Storages.getReportDir(InjectedDataStore.instance.getMcLocation());

			addIntroNote(1, "http://openeye.openmods.info");
			addIntroNote(2, "http://openeye.openmods.info");
			addIntroNote(3, "http://openeye.openmods.info/storage-policy");
			addIntroNote(4, reportDir.toURI().toString());
			addIntroNote(5, "https://github.com/OpenMods/OpenData");
			addIntroNote(6, "http://openeye.openmods.info/configuration");
			state.infoNotesDisplayed = true;
		}

		if (!state.mainMenuInfoDisplayed) {
			addMenuLine(256, ChatMessageComponent.createFromTranslationKey("openeye.main_screen.intro").setColor(EnumChatFormatting.GOLD));
			state.mainMenuInfoDisplayed = true;
		}
	}
}
