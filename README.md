# JARVIS BAUMANN — MVP V1

Fluxo: toque no núcleo → fale → SpeechRecognizer → IA (Anthropic Messages API) → TextToSpeech → conversa salva no Room.

## 1. Abrir o projeto
1. Instale o Android Studio (versão recente); o JDK 17 já vem embutido.
2. File > Open > selecione a pasta `JarvisBaumann`.
3. Aguarde o Gradle Sync (precisa de internet; baixa Gradle 8.9 e as dependências).
   Se o Studio avisar que falta o Gradle Wrapper, aceite a sugestão de gerá-lo/usá-lo.

## 2. Chave da API (nunca vai no código)
- API: Anthropic Messages API (`POST https://api.anthropic.com/v1/messages`), modelo `claude-sonnet-5-5`.
- Obter a chave: https://console.anthropic.com > API Keys (exige crédito na conta).
- No app: toque em **CHAVE API**, cole a chave e salve.
  Fica em EncryptedSharedPreferences (Android Keystore, AES-256), nunca em texto puro.

## 3. Gerar o APK
Build > Build Bundle(s) / APK(s) > Build APK(s).
Arquivo: `app/build/outputs/apk/debug/app-debug.apk`

## 4. Instalar no Redmi
Opção A (cabo): Configurações > Sobre o telefone > toque 7x na versão do MIUI/HyperOS >
Configurações adicionais > Opções do desenvolvedor > ative "Depuração USB" e "Instalar via USB".
Conecte o cabo, selecione o aparelho no Android Studio e clique em Run.
Opção B (arquivo): copie o APK para o celular, abra e permita "instalar apps desconhecidos".

Requisito: app **Google** (serviços de fala) instalado/atualizado e voz em português do Brasil
no Google Text-to-Speech.

## 5. Teste manual por módulo
1. Chave: salvar → indicador muda para "SISTEMA ONLINE".
2. Microfone: tocar no núcleo → pede permissão → estado OUVINDO.
3. Voz→texto: falar "que dia é hoje" → PROCESSANDO mostra o que foi ouvido.
4. IA: a resposta aparece na tela.
5. Texto→voz: a resposta é falada (FALANDO); tocar no núcleo interrompe.
6. Histórico: LOG mostra a conversa; "Apagar histórico" limpa.
7. Erros: modo avião → mensagem de sem conexão; chave errada → mensagem de chave recusada.

## Estrutura
app/src/main/java/com/baumann/jarvis/
- domain/ model, repository
- data/ local (Room, SecureKeyStore), remote (AnthropicClient), repository
- services/ VoiceService, AndroidVoiceService, AIService
- presentation/home/ HomeScreen, HomeViewModel, JarvisCore
Memória, comandos, pesquisa, notificações e wake word entram a partir da V1.1.
