import java.awt.FileDialog;
import java.awt.Frame;

import processing.core.*;

public class Main extends PApplet{
	public void setup() {
	    size(600,600);
	    background(0);
	    //File Dialog Box from here: http://explodingart.com/jmusic/jmtutorial/SimpleAnalysis.html
	    FileDialog fd;
	    Frame f = new Frame();
	    //open a MIDI file	               
  		fd = new FileDialog(f,"Open MIDI file or choose cancel to finish.",
                    		    FileDialog.LOAD);
  		fd.show();
  		//break out when user presses cancel	               
  		//if(fd.getFile() == null) break;
  		
  		Midi_Loader midiFile = new Midi_Loader(fd.getDirectory()+fd.getFile());
	}
	
	public void draw() {
		stroke(255);
		if (mousePressed) {
			line(mouseX,mouseY,pmouseX,pmouseY);
		}
	}
}
