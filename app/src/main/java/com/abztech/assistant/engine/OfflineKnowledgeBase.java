package com.abztech.assistant.engine;

import java.util.*;

/**
 * Curated offline encyclopedic knowledge base, facts, jokes, and conversational responses.
 * Operates with 0 KB network usage and instant retrieval.
 */
public class OfflineKnowledgeBase {

    private static class Entry {
        final List<String> triggers;
        final String answer;

        Entry(String answer, String... triggers) {
            this.answer = answer;
            this.triggers = Arrays.asList(triggers);
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<String> jokes = new ArrayList<>();
    private final List<String> funFacts = new ArrayList<>();
    private final Random random = new Random();

    public OfflineKnowledgeBase() {
        initJokes();
        initFunFacts();
        initKnowledge();
    }

    private void initJokes() {
        jokes.add("Why don't scientists trust atoms? Because they make up everything!");
        jokes.add("Why did the computer catch a cold? Because it had its Windows open!");
        jokes.add("There are 10 types of people in the world: those who understand binary, and those who don't.");
        jokes.add("Why do Java developers wear glasses? Because they don't C#!");
        jokes.add("What did the router say to the doctor? It hurts when IP!");
        jokes.add("Why was the mobile phone wearing glasses? It lost its contacts!");
        jokes.add("An SEO expert walks into a bar, bars, pub, tavern, public house, Irish pub, drinks, beer...");
        jokes.add("Why did the scarecrow win an award? Because he was outstanding in his field!");
        jokes.add("What is an astronaut's favorite part on a computer? The space bar!");
        jokes.add("How do you comfort a JavaScript bug? You console it!");
    }

    private void initFunFacts() {
        funFacts.add("Honey never spoils. Archaeologists have found pots of honey in ancient Egyptian tombs that are over 3,000 years old and still edible!");
        funFacts.add("Bananas are curved because they grow towards the sun against gravity, a process called negative geotropism.");
        funFacts.add("Octopuses have three hearts and blue blood!");
        funFacts.add("A day on Venus is longer than a year on Venus. It takes Venus 243 Earth days to rotate once, but only 225 Earth days to orbit the Sun.");
        funFacts.add("Hot water can freeze faster than cold water under certain conditions, known as the Mpemba effect.");
        funFacts.add("A single lightning bolt packs enough energy to toast 100,000 slices of bread.");
        funFacts.add("The heart of a blue whale is as large as a small car and can weigh over 400 pounds.");
        funFacts.add("Sound travels about 4.3 times faster in water than it does in air.");
    }

    private void initKnowledge() {
        // Astronomy & Physics
        add("The speed of light in a vacuum is approximately 299,792,458 meters per second, or about 300,000 km/s.", "speed of light", "how fast is light", "light speed");
        add("The average distance from the Earth to the Moon is about 384,400 kilometers (238,855 miles).", "distance to moon", "how far is the moon", "distance between earth and moon");
        add("The average distance from Earth to the Sun is approximately 149.6 million kilometers (93 million miles), known as 1 Astronomical Unit.", "distance to sun", "how far is the sun");
        add("Jupiter is the largest planet in our solar system, with a mass more than twice that of all the other planets combined.", "largest planet", "biggest planet");
        add("Mercury is the closest planet to the Sun, but Venus is the hottest due to its thick greenhouse atmosphere.", "closest planet to sun", "hottest planet");
        add("The Earth is estimated to be approximately 4.54 billion years old.", "age of earth", "how old is earth", "how old is the earth");
        add("Standard acceleration due to Earth's gravity is 9.80665 meters per second squared.", "gravity of earth", "acceleration due to gravity");

        // Geography & Capitals
        add("The capital of France is Paris.", "capital of france", "france capital");
        add("The capital of the United States is Washington, D.C.", "capital of usa", "capital of united states", "capital of america");
        add("The capital of the United Kingdom is London.", "capital of uk", "capital of england", "capital of united kingdom");
        add("The capital of Germany is Berlin.", "capital of germany", "germany capital");
        add("The capital of Japan is Tokyo.", "capital of japan", "japan capital");
        add("The capital of Canada is Ottawa.", "capital of canada", "canada capital");
        add("The capital of Australia is Canberra.", "capital of australia", "australia capital");
        add("The capital of China is Beijing.", "capital of china", "china capital");
        add("The capital of India is New Delhi.", "capital of india", "india capital");
        add("The capital of Brazil is Brasília.", "capital of brazil", "brazil capital");
        add("The capital of Italy is Rome.", "capital of italy", "italy capital");
        add("The capital of Spain is Madrid.", "capital of spain", "spain capital");
        add("The capital of Nigeria is Abuja.", "capital of nigeria", "nigeria capital");
        add("The capital of Egypt is Cairo.", "capital of egypt", "egypt capital");
        add("The capital of Russia is Moscow.", "capital of russia", "russia capital");
        add("The capital of South Africa has three capitals: Pretoria (executive), Bloemfontein (judicial), and Cape Town (legislative).", "capital of south africa");

        // Geography Superlatives
        add("Mount Everest is the highest mountain above sea level, reaching 8,848.86 meters (29,031.7 ft).", "tallest mountain", "highest mountain", "mount everest");
        add("The Nile River is traditionally considered the longest river in the world at approximately 6,650 kilometers (4,132 miles).", "longest river", "longest river in the world");
        add("The Pacific Ocean is the largest and deepest ocean on Earth, covering more than 60 million square miles.", "largest ocean", "biggest ocean");
        add("The Sahara is the largest hot desert in the world, while Antarctica is the largest desert overall.", "largest desert", "biggest desert");
        add("The Mariana Trench is the deepest oceanic trench on Earth, reaching a depth of nearly 11,000 meters (36,000 feet).", "deepest trench", "mariana trench");

        // Technology & History
        add("Android was created by Andy Rubin, Rich Miner, Nick Sears, and Chris White in 2003, and was acquired by Google in 2005.", "who created android", "who invented android", "origin of android");
        add("Apple was founded on April 1, 1976, by Steve Jobs, Steve Wozniak, and Ronald Wayne.", "who founded apple", "founders of apple");
        add("Google was founded on September 4, 1998, by Larry Page and Sergey Brin while they were Ph.D. students at Stanford University.", "who founded google", "founders of google");
        add("Java was developed by James Gosling at Sun Microsystems and released in 1995.", "who created java", "who invented java");
        add("Linux was created by Linus Torvalds in 1991 as a free and open-source operating system kernel.", "who created linux", "who invented linux");
        add("Alan Turing was a pioneering British mathematician and computer scientist who formalized the concepts of algorithm and computation.", "who is alan turing", "alan turing");
        add("Ada Lovelace is recognized as the world's first computer programmer for her work on Charles Babbage's mechanical general-purpose computer.", "first programmer", "who is ada lovelace");
        add("Apollo 11 landed humans on the Moon on July 20, 1969, with Neil Armstrong and Buzz Aldrin.", "first moon landing", "apollo 11", "when did humans land on the moon");

        // Philosophy & Witty Siri Answers
        add("The meaning of life is 42, according to The Hitchhiker's Guide to the Galaxy. But practically, it's whatever brings you purpose and joy.", "meaning of life", "what is the meaning of life");
        add("I'm an offline AI assistant created by AbzTech. I live directly on your phone and require zero internet to assist you.", "who are you", "what are you", "who made you", "who created you");
        add("I don't sleep—I'm always ready at your command!", "do you sleep", "are you asleep", "go to sleep");
        add("I'm pure Java code running on your Android device. No flesh, just smart algorithms!", "are you human", "are you real", "are you a person");
        add("Boots and cats and boots and cats and boots and cats and chk-chk-chk!", "beatbox", "can you beatbox");
        add("I'm doing fantastic! Ready to help you with anything on your device.", "how are you", "how are you doing", "how are you today");
    }

    private void add(String answer, String... triggers) {
        entries.add(new Entry(answer, triggers));
    }

    public String getRandomJoke() {
        return jokes.get(random.nextInt(jokes.size()));
    }

    public String getRandomFunFact() {
        return funFacts.get(random.nextInt(funFacts.size()));
    }

    /**
     * Looks up an answer based on trigger similarity and token overlap.
     */
    public String findAnswer(String query) {
        if (query == null) return null;
        String normalized = query.toLowerCase(Locale.ROOT).trim();

        // Exact or substring match first
        for (Entry entry : entries) {
            for (String trigger : entry.triggers) {
                if (normalized.equals(trigger) || normalized.contains(trigger)) {
                    return entry.answer;
                }
            }
        }

        // Token overlap scoring
        String[] queryTokens = normalized.split("\\s+");
        Entry bestEntry = null;
        int maxMatches = 0;

        for (Entry entry : entries) {
            for (String trigger : entry.triggers) {
                String[] triggerTokens = trigger.split("\\s+");
                int matches = 0;
                for (String tToken : triggerTokens) {
                    for (String qToken : queryTokens) {
                        if (qToken.equals(tToken) || FuzzyMatcher.similarity(qToken, tToken) > 0.85f) {
                            matches++;
                            break;
                        }
                    }
                }
                if (matches >= 2 && matches > maxMatches && (matches >= triggerTokens.length * 0.6f)) {
                    maxMatches = matches;
                    bestEntry = entry;
                }
            }
        }

        return (bestEntry != null) ? bestEntry.answer : null;
    }
}
