package com.sundaychallenge.config;

import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.ChallengeQuestion;
import com.sundaychallenge.entity.Question;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.repository.ChallengeQuestionRepository;
import com.sundaychallenge.repository.ChallengeRepository;
import com.sundaychallenge.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SampleDataInitializer seeds the default Aptitude Challenge #1 with 10 realistic questions on startup.
 * SAFE INITIALIZATION RULE: Checks specifically if "Aptitude Challenge #1" exists before inserting.
 */
@Component
public class SampleDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataInitializer.class);

    private final ChallengeRepository challengeRepository;
    private final QuestionRepository questionRepository;
    private final ChallengeQuestionRepository challengeQuestionRepository;

    public SampleDataInitializer(ChallengeRepository challengeRepository,
                                 QuestionRepository questionRepository,
                                 ChallengeQuestionRepository challengeQuestionRepository) {
        this.challengeRepository = challengeRepository;
        this.questionRepository = questionRepository;
        this.challengeQuestionRepository = challengeQuestionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String sampleTitle = "Aptitude Challenge #1";

        // Specifically check if "Aptitude Challenge #1" already exists
        if (challengeRepository.findByTitle(sampleTitle).isPresent()) {
            log.info("[DEBUG] Sample challenge '{}' already exists. Skipping initialization.", sampleTitle);
            return;
        }

        log.info("[DEBUG] Seeding sample challenge '{}' with 10 aptitude questions...", sampleTitle);

        Challenge challenge = new Challenge(
                sampleTitle,
                "Test your basic quantitative aptitude, numerical speed, and problem-solving skills.",
                Category.APTITUDE,
                Difficulty.EASY,
                15,
                10,
                100,
                true
        );
        challenge = challengeRepository.save(challenge);

        List<Question> questions = List.of(
                new Question("If 5x + 10 = 35, what is the value of x?", "3", "5", "6", "7", "B", 10, "Subtract 10 from both sides: 5x = 25. Divide by 5: x = 5."),
                new Question("A train traveling at 60 km/h passes a pole in 9 seconds. What is the length of the train in meters?", "120 m", "150 m", "180 m", "200 m", "B", 10, "Speed in m/s = 60 * (5/18) = 50/3 m/s. Length = Speed * Time = (50/3) * 9 = 150 meters."),
                new Question("If a shirt originally costs $80 and is discounted by 25%, what is the sale price?", "$55", "$60", "$64", "$70", "B", 10, "25% of 80 is 20. Sale price = 80 - 20 = $60."),
                new Question("What is the average of the first five prime numbers (2, 3, 5, 7, 11)?", "5.2", "5.6", "6.0", "6.4", "B", 10, "Sum = 2 + 3 + 5 + 7 + 11 = 28. Average = 28 / 5 = 5.6."),
                new Question("If 12 men can complete a work in 20 days, how many days will 15 men take to complete the same work?", "12 days", "16 days", "18 days", "24 days", "B", 10, "Total Man-Days = 12 * 20 = 240. Days for 15 men = 240 / 15 = 16 days."),
                new Question("What is the compound interest on $1,000 for 2 years at 10% per annum compounded annually?", "$100", "$210", "$220", "$250", "B", 10, "Amount = 1000 * (1.1)^2 = 1210. Interest = 1210 - 1000 = $210."),
                new Question("In a ratio of 3:5, if the total sum is 240, what is the value of the larger part?", "90", "120", "150", "160", "C", 10, "Total parts = 3 + 5 = 8. Each part = 240 / 8 = 30. Larger part = 5 * 30 = 150."),
                new Question("A car covers a distance of 300 km in 4 hours. What is its average speed in km/h?", "65 km/h", "70 km/h", "75 km/h", "80 km/h", "C", 10, "Average speed = Distance / Time = 300 / 4 = 75 km/h."),
                new Question("Find the missing number in the sequence: 2, 4, 8, 16, 32, __?", "48", "60", "64", "128", "C", 10, "Each term is multiplied by 2. 32 * 2 = 64."),
                new Question("What is 15% of 400?", "50", "60", "70", "80", "B", 10, "15% of 400 = (15 / 100) * 400 = 60.")
        );

        int order = 1;
        for (Question q : questions) {
            Question savedQ = questionRepository.save(q);
            ChallengeQuestion cq = new ChallengeQuestion(challenge, savedQ, order++);
            challengeQuestionRepository.save(cq);
        }

        log.info("[DEBUG] Sample challenge '{}' seeded successfully with 10 questions.", sampleTitle);
    }
}
