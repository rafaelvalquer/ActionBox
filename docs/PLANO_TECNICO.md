# ActionBox V1 — plano técnico

## Objetivo
Aplicativo Android local que transforma texto/links compartilhados em ações úteis com poucos toques.

## Stack
- Kotlin
- Jetpack Compose + Material 3
- MVVM
- Room/SQLite
- DataStore
- Coroutines + StateFlow
- Navigation Compose
- AlarmManager + Notifications
- Android Intents (Calendar, Maps, Dialer, Contacts)

## 8 ações do MVP
1. ✅ Tarefa — persiste localmente e aparece em Pendentes.
2. ⏰ Lembrete — persiste localmente e agenda notificação.
3. 📅 Compromisso — detecta título/data/hora e abre calendário preenchido.
4. 📝 Nota — salva localmente.
5. 🔖 Depois — salva links, classifica em Ler/Assistir/Comprar.
6. 📍 Endereço — abre o app de mapas via geo intent.
7. 📞 Contato — abre discador e permite criar contato sem permissão de leitura de contatos.
8. 💬 Resposta — gera 3 respostas locais por templates e copia a escolhida.

## Entrada
- Campo na Home.
- Colar da área de transferência.
- Sharesheet Android via ACTION_SEND text/plain.

## Detecção
ActionDetector usa score local por palavras-chave, URL, telefone, endereço, data e horário. O usuário pode trocar manualmente o tipo sugerido.

## Dados
Tabela Room única `actions` com type/status. Sem login, backend ou cloud.

## Navegação
- Início
- Ações (Pendentes / Notas / Histórico)
- Depois
- Configurações

## Privacidade
Nenhum conteúdo é enviado para servidor nesta V1.

## Próximas versões
- V1.1: captura de screenshot, voz, busca, tags, import/export.
- V2: parser IA opcional apenas quando a confiança local for baixa.
