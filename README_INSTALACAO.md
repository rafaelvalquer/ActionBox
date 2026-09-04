# ActionBox MVP — instalação no projeto Android Studio existente

Este pacote foi preparado para o projeto mostrado em `C:\Projetos\ActionBox` com package `com.luminor.actionbox`.

## Antes de copiar
1. Feche o Android Studio ou aguarde o Gradle terminar.
2. Faça uma cópia de segurança de `C:\Projetos\ActionBox`.
3. Não apague `local.properties`, `gradlew`, `gradlew.bat` nem `gradle/wrapper/gradle-wrapper.jar` do seu projeto atual. Este ZIP não substitui esses arquivos.

## Instalação simples
1. Extraia o conteúdo deste ZIP.
2. Copie **todo o conteúdo da pasta `ActionBox_MVP`** para `C:\Projetos\ActionBox`.
3. Quando o Windows perguntar, escolha **Substituir os arquivos no destino**.
4. Abra `C:\Projetos\ActionBox` no Android Studio.
5. Clique em **File > Sync Project with Gradle Files**.
6. Se o Android Studio pedir o SDK Android 36, clique para instalar. Também confirme JDK 17 em Settings > Build Tools > Gradle.
7. Aguarde o Gradle baixar Room, DataStore e demais dependências.
8. Execute em um emulador ou celular Android 8.0+.

## Se aparecer conflito com arquivos antigos
O projeto inicial do Android Studio pode ter criado arquivos de tema. Os arquivos deste pacote usam os mesmos nomes e devem substituí-los. Se ainda houver arquivos Kotlin duplicados em `app/src/main/java/com/luminor/actionbox/ui/theme`, mantenha apenas:
- Color.kt
- Theme.kt
- Type.kt

## Teste rápido
Na Home, teste:
- `Preciso enviar o relatório até sexta` → ✅ Tarefa
- `Me lembra amanhã às 10h de ligar para João` → ⏰ Lembrete
- `Reunião amanhã às 14h com o time` → 📅 Compromisso
- `Uma informação importante para guardar` → 📝 Nota
- `https://developer.android.com` → 🔖 Depois
- `Avenida Paulista, 1000, São Paulo` → 📍 Endereço
- `11 99999-8888` → 📞 Contato
- `Você consegue participar amanhã às 15h?` → 💬 Resposta

## Teste do compartilhamento
1. Instale o app no celular/emulador.
2. Abra Chrome ou outro app.
3. Compartilhe um texto/link.
4. Escolha **ActionBox** no Sharesheet.
5. O ActionBox abrirá com a ação já analisada.

## Lembretes
No Android 13+, permita notificações quando solicitado. O MVP usa AlarmManager local e não depende de internet.

## Build utilizado pelo pacote
- AGP 8.13.2
- Gradle 8.13
- Kotlin 2.3.21
- compileSdk/targetSdk 36
- JDK 17
- Compose BOM 2026.06.00
- Room 2.8.4
- DataStore 1.2.1
- Navigation Compose 2.9.8

Essas versões foram fixadas para evitar que uma atualização automática altere o comportamento do MVP durante os primeiros testes.
