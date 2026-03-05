package com.example.adoptions;

import org.springaicommunity.mcp.security.client.sync.AuthenticationMcpTransportContextProvider;
import org.springaicommunity.mcp.security.client.sync.oauth2.http.client.OAuth2AuthorizationCodeSyncHttpRequestCustomizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.util.List;

@SpringBootApplication
public class AdoptionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdoptionsApplication.class, args);
    }

    @Bean
    OAuth2AuthorizationCodeSyncHttpRequestCustomizer auth2AuthorizationCodeSyncHttpRequestCustomizer(
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        return new OAuth2AuthorizationCodeSyncHttpRequestCustomizer(
                authorizedClientManager, "spring"
        );
    }


    @Bean
    McpSyncClientCustomizer syncClientCustomizer() {
        return (name, spec) -> spec.transportContextProvider(
                new AuthenticationMcpTransportContextProvider());
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor
                .builder(vectorStore)
                .build();
    }

    @Bean
    PromptChatMemoryAdvisor promptChatMemoryAdvisor(DataSource dataSource) {
        var repo = JdbcChatMemoryRepository
                .builder()
                .dataSource(dataSource)
                .build();
        var mwa = MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(repo)
                .build();
        return PromptChatMemoryAdvisor
                .builder(mwa)
                .build();
    }
}

record Dog(@Id int id, String name, String description) {
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;

    AssistantController(
            ToolCallbackProvider toolCallbackProvider,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            VectorStore vectorStore, DogRepository repository,
            PromptChatMemoryAdvisor chatMemoryAdvisor,
            ChatClient.Builder ai) {

        if (false) {
            repository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name : %s, description: %s".formatted(
                        dog.id(), dog.name(), dog.description()
                ));
                vectorStore.add(List.of(dogument));
            });
        }

        var system = """
                
                You are an AI powered assistant to help people adopt a dog from the 
                adoptions agency named Pooch Palace with locations in Antwerp, Seoul, 
                Tokyo, Singapore, Paris, Mumbai, New Delhi, Barcelona, San Francisco, 
                and London. Information about the dogs availables will be presented below. 
                If there is no information, then return a polite response suggesting wes 
                don't have any dogs available.
                
                If somebody asks for a time to pick up the dog, don't ask other questions: 
                simply provide a time by consulting the tools you have available.
                
                """;
        this.ai = ai
                .defaultAdvisors(questionAnswerAdvisor,
                        chatMemoryAdvisor)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultSystem(system)
                .build();
    }

    @GetMapping("/ask")
    String ask(
//            @RequestParam String question
    ) {
        return this.ai
                .prompt()
                .user("when might i pick up Prancer from the san francisco Pooch Palace location?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "josh"))
                .call()
                .content();
    }

}


record DogAdoptionSuggestion(int id, String name) {
}