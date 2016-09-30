package org.numenta.nupic.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class RDSECompatibilityTest {
    private RandomDistributedScalarEncoder rdse;
    private RandomDistributedScalarEncoder.Builder builder;
    
    @SuppressWarnings("unchecked")
    @Test
    public void testRDSEPythonCompatibility() {
//        min = 9.0, max = 5059.0
//        resolution = 38.8461538462
//        shuffle: [358, 76, 220, 111, 128, 285, 181, 244, 26, 262, 288, 191, 171, 27, 193, 218, 207, 206, 201, 157, 101, 372, 147, 183, 261, 307, 239, 269, 67, 289, 389, 52, 175, 355, 361, 208, 198, 13, 176, 352, 114, 233, 174, 75, 214, 378, 279, 154, 146, 93, 58, 212, 357, 35, 211, 57, 104, 37, 300, 304, 83, 84, 287, 79, 322, 160, 377, 219, 232, 291, 284, 2, 323, 320, 317, 383, 240, 34, 167, 199, 125, 351, 386, 151, 109, 387, 137, 369, 41, 237, 203, 88, 7, 354, 275, 216, 44, 116, 238, 223, 62, 94, 332, 161, 48, 363, 303, 314, 98, 298, 112, 391, 164, 319, 131, 205, 278, 90, 89, 12, 398, 130, 80, 329, 209, 324, 159, 149, 51, 325, 384, 64, 242, 117, 311, 15, 293, 313, 292, 265, 297, 370, 46, 382, 327, 23, 69, 224, 359, 276, 272, 143, 309, 364, 177, 274, 368, 246, 184, 63, 227, 366, 263, 60, 86, 50, 122, 281, 91, 259, 312, 310, 349, 392, 169, 30, 365, 234, 256, 348, 126, 4, 347, 97, 0, 134, 187, 318, 168, 132, 142, 336, 200, 390, 330, 108, 180, 337, 74, 107, 43, 49, 236, 55, 393, 367, 153, 110, 158, 103, 17, 5, 356, 306, 11, 102, 71, 394, 150, 9, 92, 379, 385, 172, 135, 230, 120, 155, 271, 68, 273, 178, 38, 162, 316, 277, 113, 197, 326, 179, 189, 31, 241, 194, 255, 66, 21, 59, 362, 243, 170, 54, 248, 381, 145, 70, 6, 210, 133, 396, 39, 321, 343, 173, 380, 264, 245, 295, 188, 32, 308, 156, 106, 85, 388, 373, 18, 286, 20, 19, 182, 344, 331, 138, 302, 342, 53, 280, 267, 360, 225, 299, 341, 338, 335, 72, 165, 144, 254, 339, 353, 29, 190, 282, 215, 221, 266, 345, 195, 166, 105, 61, 231, 252, 257, 228, 334, 14, 268, 40, 290, 235, 24, 217, 73, 247, 305, 375, 333, 140, 42, 56, 22, 124, 148, 28, 33, 115, 152, 185, 196, 328, 36, 226, 118, 87, 25, 123, 8, 163, 136, 127, 47, 141, 253, 251, 45, 65, 129, 301, 340, 350, 78, 399, 395, 81, 249, 202, 222, 95, 96, 1, 374, 3, 376, 258, 229, 186, 77, 397, 121, 213, 139, 371, 10, 283, 294, 346, 270, 296, 260, 119, 192, 99, 82, 16, 100, 204, 315, 250]
//        n = 400
//        w = 21
        double resolution = 38.8461538462;
        builder = RandomDistributedScalarEncoder.builder()
            .name("enc")
            .resolution(resolution);
        rdse = builder.build();
        
        int[] exp = { 358, 76, 220, 111, 128, 285, 181, 244, 26, 262, 288, 191, 171, 27, 193, 218, 207, 206, 201, 157, 101 };
        int[] actual = rdse.getBucketValues(List.class).get(0)
            .stream()
            .mapToInt(i -> ((Integer)i).intValue())
            .toArray();
        System.out.println("actual: " + Arrays.toString(actual));
        assertTrue(Arrays.equals(exp, actual));
        
        assertEquals(21, exp.length);
        assertEquals(21, rdse.getW());
        assertEquals(400, rdse.getN());
        
        /*
            ============== dump =============
            RandomDistributedScalarEncoder:
              minIndex:   500
              maxIndex:   500
              w:          21
              n:          400
              resolution: 38.8462
              offset:     None
              numTries:   0
              name:       [38.8461538462]
            =================================
         */
        
        System.out.println("============== dump =============");
        System.out.println(rdse.toString());
        System.out.println("=================================");
        
        List<String> expectedEncoding = getFileContents("TravelTime_Value_Encoding.txt");
        List<String> inputs = getFileContents("TravelTime_Values.txt");
        for(int i = 0;i < expectedEncoding.size();i++) {
            int[] encoding = rdse.encode(Double.parseDouble(inputs.get(i)));
            String str = expectedEncoding.get(i);
            int[] expected = Arrays.stream(str.substring(1, str.length()-1).split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
            assertTrue(Arrays.equals(expected, encoding));
        }
        
     }

     private List<String> getFileContents(String fileName) {
         List<String> retVal = new ArrayList<>();
         
         InputStream is = getClass().getResourceAsStream("/".concat(fileName));
         if(is == null) {
             try {
                 is = new FileInputStream("src/test/resources/".concat(fileName));
             }catch(Exception e) { e.printStackTrace(); }
         }
         BufferedReader buf = new BufferedReader(new InputStreamReader(is));
         String line = null;
         try {
             while((line = buf.readLine()) != null) {
                 retVal.add(line.trim());
             }
         }catch(Exception e) { e.printStackTrace(); }
         finally { try { is.close(); }catch(Exception e) { e.printStackTrace(); } }
         
         return retVal;
     }
}
