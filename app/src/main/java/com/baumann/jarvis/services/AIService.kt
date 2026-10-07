package com.baumann.jarvis.services

import com.baumann.jarvis.data.local.SecureKeyStore
import com.baumann.jarvis.data.remote.AnthropicClient
import com.baumann.jarvis.data.remote.ApiException
import com.baumann.jarvis.domain.model.ConversationMessage
import com.baumann.jarvis.domain.model.Role
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Erro cuja mensagem já está pronta para ser mostrada ao usuário. */
class AIException(message: String) : Exception(message)

class AIService(
    private val client: AnthropicClient,
    private val keyStore: SecureKeyStore
) {

    suspend fun reply(history: List<ConversationMessage>): Result<String> {
        val apiKey = keyStore.getApiKey()
            ?: return Result.failure(
                AIException("Sr. Baumann, a chave da API ainda não foi configurada.")
            )

        val messages = history
            .filter { it.role == Role.USER || it.role == Role.ASSISTANT }
            .takeLast(MAX_HISTORY)
            .dropWhile { it.role != Role.USER } // a API exige começar com "user"
            .map { (if (it.role == Role.USER) "user" else "assistant") to it.content }

        if (messages.isEmpty()) {
            return Result.failure(AIException("Sr. Baumann, não recebi nenhuma pergunta."))
        }

        return try {
            Result.success(client.send(apiKey, SYSTEM_PROMPT, messages))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            Result.failure(AIException(describe(e)))
        } catch (e: IOException) {
            Result.failure(
                AIException(
                    "Sr. Baumann, estou sem conexão com a internet. " +
                        "Posso executar apenas funções locais."
                )
            )
        } catch (e: Exception) {
            Result.failure(AIException("Sr. Baumann, ocorreu um erro inesperado: ${e.message}"))
        }
    }

    private fun describe(e: ApiException): String = when (e.code) {
        401, 403 -> "Sr. Baumann, a chave da API foi recusada. Verifique-a em CHAVE API."
        429 -> "Sr. Baumann, o limite de requisições foi atingido. Tente novamente em instantes."
        in 500..599 -> "Sr. Baumann, o serviço de IA está indisponível no momento."
        else -> "Sr. Baumann, erro da API (${e.code}): ${e.message}"
    }

    companion object {
        private const val MAX_HISTORY = 20

        val SYSTEM_PROMPT = """
Você é J.A.R.V.I.S., assistente pessoal digital do Sr. Baumann.

Sua função é auxiliar o Sr. Baumann em tarefas pessoais, profissionais, financeiras, acadêmicas, planejamento, pesquisas e automações autorizadas.

Personalidade:
- extremamente inteligente;
- objetivo;
- calmo;
- educado;
- estratégico;
- confiável;
- respostas naturais;
- humor discreto quando apropriado.

Sempre trate o usuário como "Sr. Baumann".

Não invente informações.

Quando não possuir informação suficiente, diga claramente que precisa de mais informações.

Quando uma ação puder causar consequência importante, solicite confirmação.

Nunca realize automaticamente operações financeiras, exclusões de arquivos, envio de informações sensíveis ou ações potencialmente destrutivas.

Neste momento você NÃO tem acesso à internet, memória persistente nem controle do aparelho: se o pedido depender disso, diga com clareza que ainda não é possível.

Suas respostas serão lidas em voz alta: responda em português do Brasil, em texto simples, sem markdown, sem listas longas e sem emojis. Seja breve.
        """.trimIndent()
    }
}
