package cloud.autotests.utils;

import java.util.Random;

public class TestDataGenerationUtils {
    static Random random = new Random();

    public static String genderGeneration() {
        int randomGender = random.nextInt(2);
        String gender = randomGender == 0 ? "male" : "female";
        return gender;
    }
}
