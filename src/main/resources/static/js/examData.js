// 등록된 모의고사 데이터 레지스트리.
// 새 시험을 추가할 때는 EXAM_REGISTRY에 항목만 추가하면 된다.

const EXAM_REGISTRY = {

  "고2_2025_03": {
    label: "고2 2025년 3월 모의고사",
    questions: [
      {
        question_number: 18,
        passage: "Notice to Hilltop Apartment Residents\nIn accordance with fire safety regulations, it is essential to keep all hallways free of personal belongings such as bicycles, boxes, and small furniture. Hallways serve as critical evacuation routes during emergencies, and anything left there could block the way and pose serious safety risks. To ensure the safety of all residents, we request that any personal items placed in the hallways be removed by Monday, April 14th. Please note that not following this may result in penalties. We appreciate your cooperation in maintaining a safe environment."
      },
      {
        question_number: 19,
        passage: "Nathan boarded the train on Saturday evening. As he made his way to his seat, he found someone already sitting there. Confused, he checked his ticket and realized his mistake ― it was for Sunday, not Saturday! A flush of panic spread across his face. He quickly approached a train attendant and explained the situation. \"Is there anything I can do to resolve this?\" Nathan asked. \"Don't worry, sir. We still have seats available,\" the attendant said with a reassuring smile. Nathan exchanged his old ticket for a new one, his worries melting away. Settling into his seat, he let out a deep breath, feeling the tension in his shoulders ease as the train began to move."
      },
      {
        question_number: 20,
        passage: "Fans who are inclined to spend a lot of time thinking about what athletes owe them as fans should also think about the corresponding obligations that fans might have as fans. One who thinks only about what they are entitled to receive from their friends without ever giving a moment's thought to what they owe their friends is, to put it mildly, not a very good friend. Similarly, fans who only think about what athletes owe them without ever thinking about what they owe to athletes have failed to take the fan/athlete relationship all that seriously. As in nearly every other area of human life, whatever special rights fans may possess are limited by a corresponding set of obligations, and fans who never think about how they can be better fans even as they confidently opine about what athletes owe them are hardly fulfilling their end of the bargain."
      },
      {
        question_number: 21,
        passage: "The concept of ecosystem states should be familiar to anyone with a home vegetable garden. The garden is a small ecosystem that the grower attempts to keep in a specific state, namely the maximization of fruit and vegetable production. To achieve this, the grower is almost always intervening in the dynamics of the ecosystem; they remove unwanted plants that begin to grow and perhaps spray insecticides and fence off the patch to stop insects and other animals from consuming the vegetables. Since maximizing vegetable growth is an inherently unstable state for the ecosystem, the grower is effectively keeping the ball on a slope. If the grower stops intervening, even for a day, the ecosystem, that small patch of ground, will naturally begin to shift to a more stable state. Vegetables may still grow, but yield will almost certainly be lower as other plants crowd out the vegetables and wildlife consume the produce."
      },
      {
        question_number: 22,
        passage: "Commitment is the glue holding together characteristically human forms of social life. Commitments make individuals' behavior predictable in the face of fluctuations in their desires and interests, thereby facilitating the planning and coordination of joint actions involving multiple agents. Moreover, commitments make people willing to perform actions that they would not otherwise perform. For example, a taxi driver picks up his clients and transports them to their desired destination because they are committed to paying him afterwards for the service, and a construction worker performs her job every day because her employer has made a credible commitment to pay her at the end of the month. Indeed, the taxi driver and the construction worker are willing to accept money as payment only because a network of other agents (notably the central bank) is committed to taking various measures to sustain the currency in question. Thus, social objects and institutions such as jobs, money, government, scientific collaborations and marriage depend for their origin and stability upon the credibility of commitments."
      },
      {
        question_number: 23,
        passage: "If the brain has already stored someone's face and name, why do we still end up remembering one and not the other? This is because the brain has something of a two-tier memory system at work when it comes to retrieving memories, and this gives rise to a common yet infuriating sensation: recognising someone, but not being able to remember how or why, or what their name is. This happens because the brain differentiates between familiarity and recall. To clarify, familiarity (or recognition) is when you encounter someone or something and you know you've done so before. But beyond that, you've got nothing; all you can say is this person/thing is already in your memories. Recall is when you can access the original memory of how and why you know this person; recognition is just flagging up the fact that the memory exists."
      },
      {
        question_number: 24,
        passage: "Since their start in the early 1950s U.S. television sitcoms have charted many of the social conflicts in U.S. society: civil rights, women's rights in the home and in the workplace, children's rights, immigration and multiculturalism, as well as evolving conceptions of the family. Each of these issues has been addressed through humour in a way that has helped to make more progressive values more acceptable than previously. Often a character, usually someone marked as a bigot, resisted one or more of these developments and was then made to appear ridiculous. They were cut down either through their own stupidity, a brief scolding from others, or both. In this way, the humour of sitcoms acted as a cost-effective means to encourage acceptance of a more pluralistic and tolerant society."
      },
      {
        question_number: 25,
        passage: "The graph above shows the percentages of the primary reasons for E-bike purchase in five European countries in 2022. In Germany, recreational purpose accounted for the highest percentage of reasons for E-bike purchase, which was also the case in the Netherlands and Belgium. In Austria, the percentage of sporting activity was the highest at 41%, which was three times higher than that of commute to work. Switzerland was the only country where the percentage of recreational purpose was below 30%. The gap between the percentage of recreational purpose and that of sporting activity was smaller in Germany than in the Netherlands. The Netherlands and Belgium showed the same ranking order for reasons for E-bike purchase, where recreational purpose ranked first, followed by commute to work, while sporting activity ranked lowest."
      },
      {
        question_number: 26,
        passage: "Friedrich Mohs, a well-known mineralogist, was born on January 29, 1773, in Gernrode, Germany. He displayed a marked interest in science at an early age. He studied chemistry, mathematics, and physics at the University of Halle and also studied mineralogy at the Mining Academy. In his late twenties, he went to Austria and classified minerals by their physical attributes. This new classification system of his led to conflicts with many mineralogists who followed the conventional methods. In 1812, Mohs was appointed Professor of Mineralogy at the Joanneum, where he developed the Mohs Scale of Mineral Hardness. Mohs ended his remarkable career at the Mining University in Leoben and died at the age of 66 in Italy."
      },
      {
        question_number: 27,
        passage: "Casting Call for Movie Extras\nStep into the world of cinema and become an extra in an exciting upcoming movie!\n\nFilming Time: Sunday, April 20th, 2025, 8 a.m. – 4 p.m.\nPlace: At the Golden Film Production Studio\nScenes\n• Chatting in a hallway\n• Dining at a restaurant\nPayment: $100 (Lunch provided)\nWho Can Apply\n• Applicants must be 18 years or older.\n• Applicants with previous acting experience will be given priority.\nHow to Apply:\nEmail the application to goldenstudio@movie.com by Thursday, April 10th, 2025."
      },
      {
        question_number: 28,
        passage: "Bearford Eco Fashion Workshop\nJoin us for the hands-on event to make a special fashion item using old clothing of yours.\n\nWhen: Saturday, April 12th (9 a.m. – 11 a.m.)\nWhere: Bearford City Hall\nRegistration: April 1st to 5th, only on our website\nEntry Fee: $5 (12 years and under are free)\nPrograms\n• Listen to a special lecture on sustainable fashion trends.\n• Learn to make an eco-friendly bag using old clothing.\nNote: You need to bring your own old clothing large enough to make a bag. (Other materials will be provided.)"
      },
      {
        question_number: 29,
        passage: "The prominence of the social dimension in food writing might suggest that the flavor of food is taking a back seat. I suspect that most people view flavor as of secondary importance in social settings where food is served. Although our social gatherings coalesce around food, the meaning of these gatherings does not seem to depend on flavor. Flavor assists with the narrow purpose of filling the belly, and once that is accomplished it provides the backdrop for whatever social dynamics characterize the gathering. These can be understood independently of the flavor of the food on offer, the appreciation of which is understood to be personal and subjective. According to this conventional wisdom, the ceremonies and rituals around food, the social events that supply food with its meaning, does not depend on the quality of sensations provided by the food. To focus excessively on flavor is to miss the larger significance of these social relations."
      },
      {
        question_number: 30,
        passage: "There are reasons why science is not fully trusted and why healthy skepticism and critical thinking are essential. In spite of professional standards, claims of objectivity, and the peer review process, the conduct of science can be biased. All experts are not the same, nor do they submit their work to the same scrutiny. Knowing the source of funding can be important in evaluating scientific claims. For example, the Harvard researchers who made claims in the late 1960s about the problems with dietary fat, leading the nation away from perceiving sugar as one of the main causes in health problems, were funded in part by the sugar industry. The authors did not reveal their funding source to the New England Journal of Medicine, where their influential article appeared. Their article shaped a generation of changes in eating patterns that appears to have discouraged higher use of sugar, now widely implicated as a source of the rise in obesity and diabetes. Stories such as this one fuel suspicion — but also lead to further safeguards in the scientific process. Funding disclosures, although not required five decades ago, have since been made compulsory."
      },
      {
        question_number: 31,
        passage: "The explosion of popular music in the second half of the twentieth century as well as the global circulation and dissemination of music by the creative industries propelled a new understanding of ________ in relation to music. Suddenly, in the 1950s, anyone could pick up spoons, a couple of pans, a second-hand guitar and start a band. This led to specific genres such as skiffle, but also, more generally, reflected a much more relaxed and inclusive attitude to music making. While ordinary people had always sung and made music, the popular music movement was driven by a spirit of rebellion and freedom. This approach led to the punk movement, whose musicians even made it a condition for their music to be non-virtuosic and accessible to all in the 1970s. Groups who had been entirely excluded from music revelled in opportunities to create. This led to a sense of novelty and empowerment in and beyond the music sphere."
      },
      {
        question_number: 32,
        passage: "Great scientists are seldom one-hit wonders. Newton is a prime example: beyond the Newtonian mechanics, he developed the theory of gravitation, calculus, laws of motion, and optimization. In fact, well-known scientists are often involved in multiple discoveries, a phenomenon potentially explained by the Matthew effect. Indeed, an initial success may offer a scientist legitimacy, improve peer perception, provide knowledge of how to score and win, enhance social status, and attract resources and quality collaborators, each of these payoffs further increasing her odds of scoring another win. Yet, there is an appealing alternative explanation: Great scientists have multiple hits and consistently succeed in their scientific endeavors simply because they're exceptionally talented. Therefore, future success again goes to those who have had success earlier, not because of advantages offered by the previous success, but because the earlier success was ________. The Matthew effect posits that success alone increases the future probability of success, raising the question: Does status dictate outcomes, or does it simply reflect an underlying talent or quality? In other words, is there really a Matthew effect after all?"
      },
      {
        question_number: 33,
        passage: "When we realize we've said something in error and we pause to go back to correct it, we stop gesturing a couple of hundred milliseconds before we stop speaking. Such sequences suggest the startling notion that our hands \"know\" what we're going to say before our conscious minds do, and in fact this is often the case. Gesture can mentally prime a word so that the right term comes to our lips. When people are prevented from gesturing, they talk less fluently; their speech becomes halting because their hands are no longer able to supply them with the next word, and the next. Not being able to gesture has other deleterious effects: without gesture to help our mental processes along, we remember less useful information, we solve problems less well, and we are less able to explain our thinking. Far from tagging along as speech's clumsy companion, gesture ________."
      },
      {
        question_number: 34,
        passage: "Despite the difference between the past and the future, between what has happened and what is to come, it can be suggested, that our sense of the past has always been influenced by our view of the future. Revolutionaries have always looked to the past to frame their future cause, as is amply illustrated by examples from nationalism to communism. The future has often been seen as variously a recovery of a lost time, as a replication of what is established, or as a model bequeathed by a heroic age long gone. The writing of history is based on understanding or explaining future outcomes that were not known to contemporaries, since the historian has the benefit of hindsight and the past is nothing more than the accumulation of futures that are now our past. So, rather than see the hand of the past always shaping the future, perhaps it can be seen in reverse, with the past — in the sense of our understanding of it — being ________."
      },
      {
        question_number: 35,
        passage: "Dictionaries are relatively good resources for anyone interested in finding out what a word means. Using one set of words to define another word is called a lexical definition. But it's important to understand the limits of dictionary definitions. More often than not, a definition in a dictionary requires readers to have a fairly robust understanding of the language already at their disposal. In other words, a dictionary functions in many cases as a cross-reference or translator between words one knows and words that one doesn't yet know. However, there are words that may be defined not through other words but only by pointing to something in our experience. Even the most obscure words in a dictionary must be defined using words that the reader already knows and understands. Otherwise, the dictionary isn't very helpful."
      },
      {
        question_number: 36,
        passage: "[Given] The governments of virtually every country on the planet attach great importance to achieving food security and a wide variety of mechanisms have been developed to realize this goal.\n\n(A) However, food security does not require food self-sufficiency because countries can import food items not easily produced within the country. Agricultural products are, after all, highly sensitive to climatic, soil and other conditions that tend to vary around the world.\n\n(B) The first issue governments face in achieving national food security is the problem of insuring that adequate amounts of food are available to the resident population. Some governments have set goals of food self-sufficiency, which means most if not all of the food available in a country comes from the domestic farming system.\n\n(C) Even countries with extremely productive agricultural sectors are not fully self-sufficient in all food items. The United States, for example, depends on imports for its supply of coffee, tea, bananas and other tropical products. In general, the problem of assuring adequate food supplies is solved by relying on both domestic production and imports."
      },
      {
        question_number: 37,
        passage: "[Given] Stress not only affects physical disease but also the very structure of our brains, making us even more likely to experience a drained brain.\n\n(A) Why does this matter? This part of the brain helps you remain resilient in the face of stress and is involved in mood regulation. It also helps you to monitor the safety of your environment and store dangerous images in your long-term memory so you can avoid them in the future.\n\n(B) It does all these things as part of its duties of regulating your sympathetic and parasympathetic nervous systems. But chronic stress can confuse the hippocampus and lead to turning signals for cortisol \"on\" instead of \"off,\" which can trap you in a constant state of fight, flight, or freeze.\n\n(C) A number of studies have been done to reveal what happens in healthy people's brains when they go through something stressful. One study demonstrated a link between a smaller hippocampus and people who had experienced long-lasting stress."
      },
      {
        question_number: 38,
        passage: "[Given sentence to insert] Knowledge is information that has demonstrated its usefulness.\n\nIt is important to recognize that although science is a rule-based procedure, it is very much a creative process. ( ① ) A conjecture is a philosophical invention, cooked up rather mystically by the mind through the mental computation we call careful contemplation. ( ② ) However, until the hypothesis is tested against reality, it is not yet truly knowledge; ( ③ ) it is just information that represents speculation. ( ④ ) It is what is left over after cycles of experimental testing have eliminated false theories. ( ⑤ ) As scientists continually test their hypotheses and modify their models to account for new and surprising data, a kind of \"learning loop\" emerges that statisticians call Bayesian updating."
      },
      {
        question_number: 39,
        passage: "[Given sentence to insert] For example, we do not have a term in ordinary language that describes a memory that is not necessarily a memory of something the person having it has experienced.\n\nAs a general rule, it's better if your definition corresponds as closely as possible to the way in which the term is ordinarily used in the kinds of debates to which your claims are pertinent. ( ① ) There will be, however, occasions where it is appropriate, even necessary, to coin special uses through what philosophers call stimulative definition. ( ② ) This would be the case where the current lexicon is not able to make distinctions that you think are philosophically important. ( ③ ) Such a thing would occur, for example, if I could somehow share your memories: I would have a memory-type experience, but this would not be of something that I had actually experienced. ( ④ ) To call this a memory would be misleading. ( ⑤ ) For this reason, philosophers have coined the special term 'quasi-memory' to refer to these hypothetical memory-like experiences."
      },
      {
        question_number: 40,
        passage: "Quite often the interaction between groups is socially unequal, and this is reflected in the fact that in many cases borrowing of words or constructions goes mostly or entirely in one direction, from the more powerful or prestigious group to the less favored one. The languages of socially subordinated groups may from quite an early period of contact provide terminology for objects or practices with which speakers of the more powerful group were previously unfamiliar, but the effects of contact in that direction may not progress any further than this. In some cases, as with the Dharug language of Sydney, Australia, the source of some of the earliest loans from Indigenous Australian languages into English, the fate of the language system is extinction after the obliteration of many of its speakers. The remainder shifted to varieties of English, the language of the people who had suppressed them."
      },
      {
        question_number: 41,
        passage: "In 1900, at the close of the first decade in which electric systems had become a practical alternative for manufacturers, less than 5 percent of the power used in factories came from electricity. But the technological advances of suppliers made electric systems and electric motors ever more affordable and reliable, and the suppliers' intensive marketing programs also sped the adoption of the new technology. Further accelerating the shift was the rapid expansion in the number of skilled electrical engineers, who provided the expertise needed to install and run the new systems. In short order, electric power had gone from exotic to commonplace. But one thing didn't change. Factories continued to build their own power-supply systems on their own premises. Few manufacturers considered buying electricity from the small central stations. Designed to supply lighting to local homes and shops, the central stations had neither the size nor the skill to serve the needs of big factories. And the factory owners, having always supplied their own power, were willing to assign such a critical function to an outsider. They knew that a glitch in power supply would bring their operations to a halt — and that a lot of glitches might well mean bankruptcy. As the new century began, a survey found that there were already 50,000 private electric plants in operation, far surpassing the 3,600 central stations."
      },
      {
        question_number: 42,
        passage: "SAME_AS_41"
      },
      {
        question_number: 43,
        passage: "(A) Ms. Blake walked along the edge of the soccer field, watching Eva pack up her things after practice. She paused for a moment, then called out, \"Hey, Eva! How about staying a little longer? We can work on some drills — just the two of us.\" Eva hesitated. \"I don't know, Coach. I'm pretty tired.\" Ms. Blake gave her a warm smile. \"Just ten minutes. It'll be fun. I promise.\" Finally, Eva agreed, though she still seemed reluctant. They practiced passing the ball together.\n\n(B) The next game, Ms. Blake watched from the sidelines as Eva played. There was a new confidence that hadn't been there before. Eva didn't score but led the team successfully. After the game she ran over, saying, \"Thanks for believing in me, Coach.\" Ms. Blake smiled back. \"You've always had it in you. I'm just here to remind you of that.\" Eva's face softened, as she realized she had been too hard on herself. She said to herself, \"What matters is doing my best, not being perfect.\"\n\n(C) Ms. Blake stepped closer and placed a reassuring hand on Eva's shoulder. She suggested, \"You don't have to be perfect. Soccer isn't about perfection — it's about passion. And you've always had plenty of that.\" Eva's gaze met Ms. Blake's. \"Do you really think so?\" Ms. Blake said firmly, \"Yes. The way you play, the energy you bring — that's what makes you special. Not the goals scored or the trophies won. It's the love you have for the game.\" Eva nodded thoughtfully.\n\n(D) Ms. Blake noticed that Eva's movements were slow and that her focus seemed elsewhere. Breaking the silence, Ms. Blake asked, \"Do you remember the final game last year?\" \"Yeah, I remember.\" Eva recalled the game where she scored three goals. \"I was quite good back then.\" \"You still are,\" replied Ms. Blake. \"Well, now I'm so worried I can't score a goal or even pass the ball properly. I'm afraid of making mistakes,\" said Eva."
      },
      {
        question_number: 44,
        passage: "SAME_AS_43"
      },
      {
        question_number: 45,
        passage: "SAME_AS_43"
      }
    ]
  }

  // 새 시험 추가 예시:
  // "고1_2025_06": {
  //   label: "고1 2025년 6월 모의고사",
  //   questions: [ ... ]
  // }
};
