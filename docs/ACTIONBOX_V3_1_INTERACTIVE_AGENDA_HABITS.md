# ActionBox V3.1 — Interactive Agenda & Habits

Implementação do plano V3.1 sobre a base `feature/actionbox-v3-motion-first`.

## Objetivos da entrega

1. Remover `Criar` da navegação inferior e concentrar a criação no Smart Capture da Home.
2. Transformar Rotinas em um tracker de ocorrências realmente interativo.
3. Substituir a tela de detalhes por um editor completo de ações.
4. Manter o estilo Motion First, dark mode e acessibilidade da V3.
5. Preservar o schema Room existente: os campos necessários já existem em `ActionEntity`.

## Implementação por fase

### Fase 1–2 — navegação

- Bottom navigation reduzida para `Hoje | Agenda | Organizar | Depois`.
- Removida a rota `capture` do `NavHost`.
- Removido o botão central elevado e sua lógica especial.
- Itens distribuídos igualmente.
- Seleção usa cor, escala discreta e indicador inferior animado.
- O `CaptureScreen.kt` permanece no projeto apenas como componente legado, sem destino de navegação.

### Fase 3–4 — Rotinas

`HabitCard.kt` agora representa cada dia com estado explícito:

- `✓` verde: ocorrência concluída;
- `○` roxo: ocorrência prevista e disponível;
- `○` cinza: ocorrência futura e bloqueada;
- `·` cinza claro: dia fora da recorrência.

Comportamento:

- ocorrência passada ou atual pode ser marcada/desmarcada;
- ocorrência futura não é clicável;
- haptic feedback ao marcar/desmarcar quando habilitado;
- `AnimatedCheck` faz a transição visual;
- progresso mensal recalcula imediatamente;
- sequência usa somente ocorrências da recorrência, nunca dias corridos;
- aumento da sequência recebe microanimação de escala.

A regra de sequência foi extraída para `HabitStreakCalculator`, com testes unitários para rotina seg/qua/sex, quebra da sequência e dias intermediários sem rotina.

### Fase 5 — estado de edição

Criado `ActionEditState.kt` com:

- tipo;
- título;
- descrição/notas;
- data;
- hora;
- prioridade;
- recorrência;
- dias da recorrência semanal;
- lembrete.

O estado é um draft local. Nenhuma alteração é persistida enquanto o usuário apenas abre sheets ou edita campos. A escrita no Room ocorre somente em `Salvar`.

### Fase 6 — update do Room

`ActionDao` recebeu `@Update suspend fun update(entity: ActionEntity)`.

`ActionRepository` expõe `update(entity)`.

Não houve mudança de schema e, portanto, não foi criada nova migration.

### Fase 7 — reagendamento de alarmes

`ActionViewModel.updateAction()` executa a sequência:

1. cancela o alarme antigo pelo `id`;
2. persiste a entidade atualizada;
3. chama `scheduleIfNeeded()` para a nova configuração;
4. emite feedback de sucesso.

Isso cobre mudança de data/hora, mudança de antecedência e conversão de Lembrete para Tarefa quando o aviso é removido.

### Fase 8 — Action Editor

Criado `ActionEditorScreen.kt`, usado pela rota `action/{id}` tanto na Home quanto na Agenda.

A tela evita cards grandes e usa fundo limpo, linhas, divisores, ícones e chips pequenos.

Estrutura:

- cabeçalho com voltar / cancelar / salvar / menu;
- chip de tipo;
- título em `BasicTextField` sem borda, 28sp Bold;
- Data;
- Hora e lembrete;
- Repetir;
- Prioridade;
- Notas.

`Salvar` só aparece quando o draft está diferente do estado inicial.

Ao voltar com alterações existe confirmação de descarte.

### Fase 9 — Tipo, Data, Hora e Lembrete

Bottom sheets criados:

- `ActionTypeSheet.kt`
- `DateSheet.kt`
- `TimeReminderSheet.kt`

Conversão inicial suportada:

- Tarefa;
- Lembrete;
- Compromisso.

Tipos com estrutura própria ficam bloqueados para conversão.

Data possui atalhos Hoje, Amanhã, Fim de semana, Escolher data e Sem data.

Hora possui atalhos 09:00, 10:00, 14:00, 19:00, TimePicker e Sem horário.

Lembrete possui Sem lembrete, Na hora, 10 min, 30 min, 1 hora e 1 dia antes.

### Fase 10 — Recorrência, Prioridade e Notas

Bottom sheets criados:

- `RecurrenceSheet.kt`
- `PrioritySheet.kt`
- `NotesSheet.kt`

Recorrência:

- Não repetir;
- Todo dia;
- Toda semana;
- Todo mês;
- Personalizado com seleção dos sete dias.

Prioridade:

- Baixa;
- Normal;
- Alta.

Notas editam o campo `description` existente.

### Fase 11 — integração Home/Agenda

A rota única `action/{id}` abre `ActionEditorScreen`.

Após salvar, as telas observam os mesmos `Flow`s do Room e refletem a atualização automaticamente, sem reiniciar Activity ou app.

Compromissos mantêm a ação `Adicionar ao calendário do celular`.

### Fase 12 — motion e haptics

- AnimatedContent em valores e tipo;
- press scale em linhas interativas;
- haptic em salvar, concluir e marcar rotina;
- AnimatedCheck no tracker;
- animação de sequência;
- shared bounds preservado na transição ação → editor.

### Fase 13 — testes e regressão

Testes adicionados:

- `HabitStreakCalculatorTest`;
- `ActionEditStateTest`.

O pipeline Android CI continua executando:

- `testDebugUnitTest`;
- `lintDebug`;
- `assembleDebug`.

## Menu do editor

O menu `⋮` possui:

- Duplicar;
- Arquivar;
- Excluir.

Excluir exige confirmação.

## Checklist manual de regressão

- [ ] Criar tarefa para amanhã e editar para hoje.
- [ ] Editar horário e confirmar atualização na Agenda.
- [ ] Converter Tarefa → Lembrete.
- [ ] Converter Lembrete → Tarefa e confirmar que o alarme anterior não dispara.
- [ ] Alterar recorrência semanal.
- [ ] Marcar seg/qua/sex e verificar sequência 3.
- [ ] Desmarcar uma ocorrência anterior e verificar quebra/redução da sequência.
- [ ] Tentar tocar uma ocorrência futura e confirmar que não muda.
- [ ] Fechar e reabrir o app e confirmar persistência.
- [ ] Editar compromisso e usar `Adicionar ao calendário do celular`.
- [ ] Validar light/dark mode.
- [ ] Voltar durante edição e validar confirmação de descarte.
- [ ] Validar Duplicar, Arquivar e Excluir.
- [ ] Validar TalkBack/content descriptions nos controles principais.

## Versão

- `versionCode = 4`
- `versionName = 3.1.0`

Entrega: **ActionBox V3.1 — Interactive Agenda & Habits**.
