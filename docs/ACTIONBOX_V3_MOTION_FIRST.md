# ActionBox V3 — Motion First

Implementação visual da V3 baseada no princípio **clean + fluida + inteligente + física**, preservando a lógica funcional, domínio e persistência já existentes sempre que possível.

## Branch

`feature/actionbox-v3-motion-first`

## Direção do produto

A assinatura visual da V3 é a transformação de informação em ação. O Smart Capture permanece no mesmo contexto visual enquanto passa pelos estados `idle → analyzing → result → edit`, evitando formulários longos e telas intermediárias desnecessárias.

## Status por fase

| Fase | Entrega | Status |
| --- | --- | --- |
| 0 | Toolchain estabilizada | ✅ |
| 1 | Design System | ✅ |
| 2 | Nova Home | ✅ |
| 3 | Smart Capture Motion | ✅ |
| 4 | Nova Agenda | ✅ |
| 5 | Organizar + Depois | ✅ |
| 6 | Shared transitions | ✅ |
| 7 | Polimento visual, haptics, dark mode e accessibility baseline | ✅ |

## 0. Toolchain

Base congelada para reduzir diferenças de ambiente:

- AGP `8.13.2`
- Kotlin `2.3.21`
- Gradle `8.13`
- JDK `21` executando Gradle no CI
- JVM target `17`
- Compose BOM `2026.06.00`

O CI executa testes unitários, Android Lint e `assembleDebug`.

## 1. Design System

Nova estrutura em `ui/designsystem/` com:

- paleta refinada e cores semânticas;
- tipografia completa;
- shapes padronizados;
- spacing tokens;
- Material Icons centralizados;
- componentes reutilizáveis (`ActionCard`, `ActionButton`, `ActionChip`, `ActionHeader`, `ActionInput`, `ActionBadge`, `ActionEmptyState`, `ActionSegmentedControl`).

O roxo `#6157F5` passa a ser a assinatura principal. Cores por categoria são usadas em pequenos elementos e estados, não como preenchimento integral das telas.

## 2. Home

A Home prioriza:

1. marca e ajustes;
2. saudação contextual;
3. Hero Smart Capture;
4. resumo compacto do dia;
5. timeline/lista limpa das ações.

Os cards estatísticos grandes foram removidos da hierarquia principal em favor de uma linha curta de resumo.

## 3. Smart Capture

O componente antigo foi dividido em arquivos menores:

- `CaptureFlow.kt`
- `CaptureIdle.kt`
- `CaptureAnalyzing.kt`
- `CaptureResult.kt`
- `CaptureEditor.kt`

O container se transforma visualmente entre estados. A análise usa pulso no ícone e o resultado apresenta o tipo, título, data/hora e confiança de forma compacta.

Detalhes avançados são editados sob demanda e usam `ModalBottomSheet`, reduzindo a quantidade de formulário visível.

## 4. Agenda

A Agenda agora possui:

- segmented control `Mês / Lista`;
- calendário mensal com dots semânticos por categoria;
- timeline vertical do dia;
- `AnimatedContent` na troca de mês;
- swipe horizontal para mês anterior/próximo;
- acesso ao detalhe da ação;
- conclusão com feedback visual e haptic quando habilitado.

## 5. Organizar

`Projetos / Listas / Rotinas / Notas` usam segmented control.

### Projetos

Cards ricos exibem progresso, percentual, próxima tarefa e acesso ao detalhe do projeto.

### Listas

Cards exibem progresso, itens concluídos e ações de finalizar/reabrir.

### Rotinas

O calendário mensal foi refinado e inclui progresso e sequência atual, mantendo a gamificação discreta.

## 6. Depois

A tela passou a se comportar como coleção:

- cards com categoria, domínio e data relativa;
- toque abre detalhe;
- swipe para arquivar;
- menu contextual com abrir, copiar, compartilhar, arquivar e excluir.

## 7. Motion e detalhes

Tokens de motion foram centralizados em `ui/motion/`.

- press effect com spring;
- feedback animado de check;
- transições de navegação rápidas;
- `SharedTransitionLayout` no nível do `NavHost`;
- `sharedBounds` para ação → detalhe, projeto → detalhe e item salvo → detalhe;
- haptics somente em ações relevantes;
- edge-to-edge via `enableEdgeToEdge()` e insets por tela;
- light/dark theme com paleta própria;
- navegação com semântica e `contentDescription` nos controles principais.

## Navegação inferior

Mantém os cinco conceitos:

- Hoje
- Agenda
- Criar
- Organizar
- Depois

O botão `Criar` fica destacado e levemente elevado no centro. Ícones de interface usam Material Icons; emojis ficam reservados para personalidade, estados vazios e conteúdo.

## Telas grandes

As telas continuam limitando a largura de conteúdo com `widthIn`, o que preserva legibilidade em tablets e janelas maiores. A adoção de Material 3 Adaptive pode ser feita em uma evolução posterior sem bloquear esta V3 mobile-first.

## Validação

Antes de integrar na branch principal, validar:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Fluxos manuais prioritários:

1. `Me lembra amanhã às 14h de falar com Dantas`
2. `Academia segunda, quarta e sexta às 19h`
3. `Projeto viagem: passagem, hotel e seguro`
4. `Ir ao mercado e comprar carne, pão e leite`
5. abrir uma ação da Home e voltar;
6. abrir um projeto e voltar;
7. abrir um item de Depois e voltar;
8. alternar tema claro/escuro;
9. navegar entre meses por swipe.
