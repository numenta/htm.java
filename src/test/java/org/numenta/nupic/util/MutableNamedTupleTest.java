package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class MutableNamedTupleTest {
    
    @Test
    public void testPutOnEmptyTuple() {
        MutableNamedTuple nt = new MutableNamedTuple();
        nt.put("ace", "Very good");
        
        assertEquals("Very good", nt.get("ace"));
        assertEquals("Very good", nt.values().iterator().next());
        
        String[][] elementsToAdd = {
            { "ace", "Very good" },
            { "act", "Take action" },
            { "add", "Join (something) to something else" },
            { "age", "Grow old" },
            { "ago", "Before the present" },
            { "aid", "Help, assist, or support" },
            { "aim", "Point or direct" },
            { "air", "Invisible gaseous substance" },
            { "all", "Used to refer to the whole quantity" },
            { "amp", "Unit of measure for the strength of an electrical current" },
            { "and", "Used to connect words" }, 
            { "ant", "A small insect" },
            { "any", "Used to refer to one or some of a thing" },
            { "ape", "A large primate" },
            { "apt", "Appropriate or suitable in the circumstances" },
            { "arc", "A part of the circumference of a curve" },
            { "are", "Unit of measure, equal to 100 square meters" },
            { "ark", "The ship built by Noah" },
            { "arm", "Two upper limbs of the human body" },
            { "art", "Expression or application of human creative skill" },
            { "ash", "Powdery residue left after the burning" },
            { "ask", "Say something in order to obtain information" },
            { "asp", "Small southern European viper" },
            { "ass", "Hoofed mammal" },
            { "ate", "To put (food) into the mouth and swallow it" },
            { "atm", "Unit of pressure" },
            { "awe", "A feeling of reverential respect" },
            { "axe", "Edge tool with a heavy bladed head" },
            { "aye", "An affirmative answer" } 
        };
        
        for(String[] elem : elementsToAdd) {
            nt.put(elem[0], elem[1]);
        }
        
        assertEquals(29, elementsToAdd.length);
        assertEquals(29, nt.keys().length);
        assertEquals(29, nt.values().size());
        
        String[] keys = new String[elementsToAdd.length];
        Object[] vals = new Object[elementsToAdd.length];
        
        Random r = new FastRandom(42);
        for(int i = 0;i < 5;i++) {
            int random = r.nextInt(29);
            assertEquals(nt.get(keys[random]), vals[random]);
        }
        
    }

    @Test
    public void testPut_ReplaceExisting() {
        String[][] elementsToAdd = {
            { "ace", "Very good" },
            { "act", "Take action" },
            { "add", "Join (something) to something else" },
            { "age", "Grow old" },
            { "ago", "Before the present" },
            { "aid", "Help, assist, or support" },
            { "aim", "Point or direct" },
            { "air", "Invisible gaseous substance" },
            { "all", "Used to refer to the whole quantity" },
            { "amp", "Unit of measure for the strength of an electrical current" },
            { "and", "Used to connect words" }, 
            { "ant", "A small insect" },
            { "any", "Used to refer to one or some of a thing" },
            { "ape", "A large primate" },
            { "apt", "Appropriate or suitable in the circumstances" },
            { "arc", "A part of the circumference of a curve" },
            { "are", "Unit of measure, equal to 100 square meters" },
            { "ark", "The ship built by Noah" },
            { "arm", "Two upper limbs of the human body" },
            { "art", "Expression or application of human creative skill" },
            { "ash", "Powdery residue left after the burning" },
            { "ask", "Say something in order to obtain information" },
            { "asp", "Small southern European viper" },
            { "ass", "Hoofed mammal" },
            { "ate", "To put (food) into the mouth and swallow it" },
            { "atm", "Unit of pressure" },
            { "awe", "A feeling of reverential respect" },
            { "axe", "Edge tool with a heavy bladed head" },
            { "aye", "An affirmative answer" } 
        };
        
        String[] keys = new String[elementsToAdd.length];
        Object[] vals = new Object[elementsToAdd.length];
        int i = 0;
        for(String[] elem : elementsToAdd) {
            keys[i] = elem[0];
            vals[i++] = elem[1];
        }
        
        MutableNamedTuple nt = new MutableNamedTuple(keys, vals);
        
        // First test that it works the same as the NamedTuple parent class
        for(String[] elem : elementsToAdd) {
            assertEquals(nt.get(elem[0]), elem[1]);
        }
        
        int arcIndex = 15;
        
        nt.put("arc", "arc of the covenant");
        
        assertEquals("arc of the covenant", nt.get("arc"));
        
        List<Object> values = new ArrayList<>(nt.values());
        
        assertEquals("arc of the covenant", values.get(arcIndex));
        
        i = 0;
        for(String elem : nt.keys()) {
            if(i == arcIndex) {
                assertEquals(nt.get(elem), "arc of the covenant");
            }else{
                assertEquals(nt.get(elem), elementsToAdd[i][1]);
            }
            i++;
        }
    }
    
    @Test
    public void testPut_InsertNonExisting() {
        String[][] elementsToAdd = {
            { "ace", "Very good" },
            { "act", "Take action" },
            { "add", "Join (something) to something else" },
            { "age", "Grow old" },
            { "ago", "Before the present" },
            { "aid", "Help, assist, or support" },
            { "aim", "Point or direct" },
            { "air", "Invisible gaseous substance" },
            { "all", "Used to refer to the whole quantity" },
            { "amp", "Unit of measure for the strength of an electrical current" },
            { "and", "Used to connect words" }, 
            { "ant", "A small insect" },
            { "any", "Used to refer to one or some of a thing" },
            { "ape", "A large primate" },
            { "apt", "Appropriate or suitable in the circumstances" },
            { "arc", "A part of the circumference of a curve" },
            { "are", "Unit of measure, equal to 100 square meters" },
            { "ark", "The ship built by Noah" },
            { "arm", "Two upper limbs of the human body" },
            { "art", "Expression or application of human creative skill" },
            { "ash", "Powdery residue left after the burning" },
            { "ask", "Say something in order to obtain information" },
            { "asp", "Small southern European viper" },
            { "ass", "Hoofed mammal" },
            { "ate", "To put (food) into the mouth and swallow it" },
            { "atm", "Unit of pressure" },
            { "awe", "A feeling of reverential respect" },
            { "axe", "Edge tool with a heavy bladed head" },
            { "aye", "An affirmative answer" } 
        };
        
        String[] keys = new String[elementsToAdd.length];
        Object[] vals = new Object[elementsToAdd.length];
        int i = 0;
        for(String[] elem : elementsToAdd) {
            keys[i] = elem[0];
            vals[i++] = elem[1];
        }
        
        MutableNamedTuple nt = new MutableNamedTuple(keys, vals);
        
        // First test that it works the same as the NamedTuple parent class
        for(String[] elem : elementsToAdd) {
            assertEquals(nt.get(elem[0]), elem[1]);
        }
        
        int arbIndex = 29;
        
        nt.put("arb", "Act of interaction with exchange to make profit");
        
        assertEquals("Act of interaction with exchange to make profit", nt.get("arb"));
        
        List<Object> values = new ArrayList<>(nt.values());
        
        assertEquals("Act of interaction with exchange to make profit", values.get(arbIndex));
        
    }

}
