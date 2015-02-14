import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
/**
 * Based on code by Sport4Minus: https://gist.github.com/sport4minus/2971754
 */
public class Midi_Loader {
	/**
	*loads a midi file from disk and stores all notes in a nested Array List, using the Helper class "Note"
	* needs some cleanup though
	*/
	ArrayList<ArrayList<Measure>> tracks;
	long maxTicks = 0;
	final boolean DO_PRINT = false;
	int timeSignature;
	 
	Midi_Loader(String fileName) {
		//Notes are stored in this nested array list to mirror the track-note structure (depending on type of midi file)
	    tracks = new ArrayList<ArrayList<Measure>>();
	    Track[] trx;
	 
	    try {
	    	//Getting Tracks
	    	System.out.println("File is: " + fileName);
	    	File myMidiFile = new File(fileName);
	    	Sequence mySeq = MidiSystem.getSequence(myMidiFile);
	    	trx = mySeq.getTracks();
	 
	    	//Getting Measures & Notes for each track
	    	for (int i = 0; i < trx.length; i++) {
	    		ArrayList<Measure> trackToMeasure = new ArrayList<Measure>();//Array of measures for this track. Each measure will contain notes.
	    		tracks.add(trackToMeasure);
	    		Track t = trx[i];//t is the raw track info.
	    		if (DO_PRINT) {
	    			System.out.println("track ");
	    			System.out.println("length:"+ t.ticks());
	    			System.out.println("num events:" + t.size());
	    		} 
	    		int counter = 0;
	    		//Get the time signature
	    		while (t.size () > 0 && counter < t.size()) {
	    			counter ++;
	    			/**
	    			 * Retrieving time signature
	    			 */
	    			if (t.get(0).getMessage() instanceof MetaMessage) {
	    				MetaMessage m = (MetaMessage)(t.get(0).getMessage());
	    				if(m.getType() == 0x58){
	    					decodeMessage(m);
		    				//System.out.println(timeSignature);
		    				counter = 0;
			    			break;
	    				}
	    				else if(m.getType() == 0x03){
	    					//TODO: Get Track Name.
	    				}
	    				if(counter == t.size()){
	    					counter = 0;
	    					break;
	    				}
	    			}
	    		}
	    		System.out.println("Done searching for time signature");
	    		//iterate over the vector, and remove each handled event.
	    		if(DO_PRINT)System.out.println("Size is: " + t.size() + " Counter is: " + counter);
	    		while (t.size () > 0 && counter < t.size()) {
	    			counter ++;
	    			if (t.get(0).getMessage() instanceof ShortMessage) {
	    				ShortMessage s = (ShortMessage)(t.get(0).getMessage());
	    				//find note on events
	    				if (s.getCommand() == ShortMessage.NOTE_ON) {
	    					if (DO_PRINT)System.out.println(s.getCommand() + " " + s.getChannel() +" " + s.getData1() + " " + s.getData2());              
	    						//store all the values temporarily in order to find the associated note off event
	    						long startTime = t.get(0).getTick();
	    						long endTime = 0;
	    						int ch = s.getChannel();
	    						int pitch = s.getData1();
	    						int vel = s.getData2();
	              
	    						//if the first note has zero velocity (== noteOff), remove it
	    						if (vel == 0) {
	    							t.remove(t.get(0));
	    						} 
	              
	    						else {
	    							//start to look for the associated note off
	    							for (int j = 0; j < t.size(); j++) {
	    								if (t.get(j).getMessage() instanceof ShortMessage) {
	    									ShortMessage s2 = (ShortMessage)(t.get(j).getMessage());
	    									//two types to send a note off... either as a clean command or as note on with 0 velocity
	    									if ((s2.getCommand() == ShortMessage.NOTE_OFF) || s2.getCommand() == ShortMessage.NOTE_ON) {
	    										//compare to stored values, sending a note off with same channel and pitch means to stop the note
	    										if (s2.getChannel() == ch && s2.getData1() == pitch && s2.getData2() == 0) {
	    											//calculate note duration
	    											endTime = t.get(j).getTick();
	    											//extend maxticks, so we know when the last midi event happened (sometimes tracks are much longer than the last note
	    											if (endTime > maxTicks)maxTicks = endTime;
	    											//create a new "Note" instance, store it
	    											Note n = new Note(startTime, endTime-startTime, ch, vel, pitch);
	    											if(DO_PRINT)System.out.println("Start time is: " + startTime);
	    											int measureNum = (int) Math.floor(startTime / timeSignature);
	    											ArrayList<Measure> currentTrack = tracks.get(i);
	    											if(DO_PRINT)System.out.println("Track size is: " + currentTrack.size());
	    											if(currentTrack.isEmpty()){
	    												System.out.println("Track's first measure number is: " + measureNum);
	    												currentTrack.add(new Measure(measureNum, n));//Add measure
	    											}
	    											else if(currentTrack.get(currentTrack.size() - 1).measureNumber < measureNum){//Measure doesn't exist.
	    												System.out.println("Measure number: " + measureNum);
	    												currentTrack.add(new Measure(measureNum, n));//Add measure
	    											}
	    											else{
	    												//TODO: Only enters here once. It might be creating a measure for every note or just skipping or something.
	    												System.out.println("Adding additional notes to measure");
	    												currentTrack.get(currentTrack.size() - 1).notes.add(n);//Add note to measure
	    											}
	    											System.out.println(currentTrack.get(currentTrack.size() - 1).notes.size());
	    											if(DO_PRINT)System.out.println("Measure number is: " + measureNum);
//	    											trackAsList.add(n);
	    											t.remove(t.get(0));
	    											break;
	    										}
	    									}
	    								}
	    							}
	    						}
	    						//remove event when done
	    						t.remove(t.get(0));
	    				}
	    				else {
	    					//remove events which are shortmessages but not note on (e.g. control change)
	    					t.remove(t.get(0));
	    				}
	    			}
	    			else {
	    				//remove events we aren't concerned with
	    				t.remove(t.get(0));
	    			}
	    		}
	    	}
	    }
	    catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
//	ArrayList<Note> trackAsArrayList(int i) {
//		return tracks.get(i);
//	    // return null;
//	}
	int numTracks() {
		return tracks.size();
	}
	/**
	 * Dump code from http://www.jsresources.org/examples/DumpReceiver.java.html,
	 * Midi data from http://www.mobilefish.com/tutorials/midi/midi_quickguide_specification.html
	 */
	public int decodeMessage(MetaMessage message)
	{
		byte[]	abData = message.getData();
		String	strMessage = "No Time Signature";
		if(message.getType() == 0x58){
			strMessage = "Time Signature: "
					+ (abData[0] & 0xFF) + "/" + (1 << (abData[1] & 0xFF))
					+ ", MIDI clocks per metronome tick: " + (abData[2] & 0xFF)
					+ ", 1/32 per 24 MIDI clocks: " + (abData[3] & 0xFF);
			timeSignature = (abData[0] & 0xFF);
			System.out.println(strMessage);
		}
		return timeSignature;
	}
}
/**
*Helper Class Measure
*/
class Measure {
	long start;
	ArrayList<Note> notes;
	int measureNumber;
 
	Measure(int theMeasure, Note newNote) {
		notes = new ArrayList<Note>();
		notes.add(newNote);
		measureNumber = theMeasure;
	}
	public void addNote(Note newNote){
		notes.add(newNote);
	}
	public ArrayList<Note> getNotes(){
		return notes;
	}
} 
/**
*Helper Class Note, stores start time, end time, channel, pitch and duration
*/
class Note {
	long start;
	long duration;
 
	int channel;
	int velocity;
	int pitch;
 
	Note(long theStart, long theDuration, int theChannel, int theVelocity, int thePitch) {
		start = theStart;
		channel = theChannel;
		pitch = thePitch;
		velocity = theVelocity;
		duration = theDuration;
	}
}