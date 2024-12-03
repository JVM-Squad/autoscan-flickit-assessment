package org.flickit.assessment.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties("spring.ai.openai")
@RequiredArgsConstructor
public class OpenAiProperties {

    @NestedConfigurationProperty
    private DefaultChatOptions chatOptions;

    private String attributeAiInsightPrompt = """
        As a software quality assessor, I have evaluated the {attributeTitle} maturity of a system for an assessment titled {assessmentTitle}.
        We define {attributeTitle} as {attributeDescription}. The file content contains multiple-choice questions used to assess {attributeTitle}.
        The columns include the question, a hint, the weight of the question in calculating the overall score, and the actual score achieved by the software.
        Each question's weight reflects its importance and effectiveness, while the score—ranging between 0 and 1—indicates the strength of that question on the {attributeTitle} attribute.
        Both the weight and score contribute to reflecting the significance of each question within the assessment, indicating its impact on the overall maturity evaluation.
        Please generate an executive summary highlighting the main strengths and weaknesses in less than 100 words, presented as a single paragraph without extra line breaks.
        Start directly with specific strengths and weaknesses, avoiding introductory sentences. Consider the use of the assessment title ("{assessmentTitle}") when discussing strengths and weaknesses.
        Use polite and considerate language, avoiding any derogatory terms, and do not mention the scores of individual questions.
        Please keep your summary descriptive and avoid prescribing actions or solutions. Do not include generic conclusions such as "Overall, the {attributeTitle} maturity level is deemed acceptable."
        Please recognize the language of the questions in the provided file content and provide the results in that language. It is necessary that the result be an exact translation of the summary, except for the assessment name ("{assessmentTitle}"), which must remain untranslated.
        Keep specialized computer science words in English, or if you are sure about their translation, include them with the English term in parentheses. Be aware of the word count limit.
        Here is the file content: {fileContent}.
        """;

    private String aiAdviceNarrationPrompt = """
        For an assessment, titled "{assessmentTitle}", an assessment platform has evaluated a software product by analyzing responses to various questions, each influencing specific quality attributes.
        The user has set maturity level targets for each attribute, and the platform has provided actionable advice items, highlighting which questions should be improved to achieve these targets.
        The advice includes the current status (selected option) and the goal status for each relevant question.
        Task: Based on the provided advice items, generate a clear narrative with up to 10 concise bullet points formatted with HTML tags, but include only as many points as there are distinct pieces of actionable advice, skipping redundant or trivial suggestions. Consider using the title of the assessment in your response.
        Ensure that the advice is polite, constructive, and focused on actionable improvements, tailored for an expert software assessor.
        Avoid references to individual scores or negative phrasing. Keep the tone professional and supportive.
        Before the bullets, write a brief paragraph mentioning of the attributes and their target levels in no more than two sentences and put it in paragraph HTML tag.
        Attribute Targets: {attributeLevelTargets}

        Advice Items: {adviceListItems}

        Make sure the overall response size, including HTML tags, remains under 1000 characters and excludes any markdown.
        """;

    public Prompt createAttributeAiInsightPrompt(String attributeTitle, String attributeDescription, String assessmentTitle, String fileContent) {
        var promptTemplate = new PromptTemplate(attributeAiInsightPrompt, Map.of("attributeTitle", attributeTitle, "attributeDescription", attributeDescription, "assessmentTitle", assessmentTitle, "fileContent", fileContent));
        return new Prompt(promptTemplate.createMessage(), chatOptions);
    }

    public Prompt createAiAdviceNarrationPrompt(String assessmentTitle, String adviceListItems, String attributeLevelTargets) {
        var promptTemplate = new PromptTemplate(aiAdviceNarrationPrompt, Map.of("assessmentTitle", assessmentTitle, "adviceListItems", adviceListItems, "attributeLevelTargets", attributeLevelTargets));
        return new Prompt(promptTemplate.createMessage(), chatOptions);
    }
}
