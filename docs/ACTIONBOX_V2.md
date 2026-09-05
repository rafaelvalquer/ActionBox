# ActionBox 2.0

A versão 2 transforma o ActionBox em um organizador pessoal orientado por intenção, mantendo a captura por linguagem natural como ponto central.

## Navegação

- **Hoje**: captura inteligente, resumo do dia, tarefas e rotinas.
- **Agenda**: visualização mensal e por lista.
- **Criar**: captura em tela cheia com editor antes de salvar.
- **Organizar**: projetos, listas, rotinas e notas.
- **Depois**: links e conteúdos salvos.

## Tipos principais

- Tarefa
- Lembrete
- Compromisso
- Nota
- Lista
- Projeto

Recorrência, prioridade, horário e aviso são propriedades editáveis e não novos tipos de ação.

## Recorrência

Ações podem ser diárias, semanais ou mensais. A conclusão é registrada por ocorrência em `action_completions`, permitindo manter o histórico de dias concluídos sem encerrar a ação recorrente.

## Listas

Listas usam `action_lists` e `list_items`. Cada item pode ser marcado individualmente. Uma lista com data também gera uma Action do tipo `LIST` para aparecer na Agenda.

## Projetos

Projetos usam `projects`. Itens identificados durante a captura são criados como tarefas vinculadas por `projectId`, permitindo cálculo de progresso.

## Banco local

Room foi atualizado da versão 1 para 2 com migration preservando dados existentes. Novas tabelas:

- `projects`
- `action_lists`
- `list_items`
- `action_completions`

A tabela `actions` recebeu campos de descrição, prioridade, recorrência, aviso e projeto.

## Exemplos

`Ir ao mercado e comprar carne, pão e leite`

→ Lista **Mercado** com três itens.

`Academia todos os dias às 19h`

→ Tarefa diária com histórico de conclusão.

`Treinar segunda, quarta e sexta às 7h`

→ Tarefa recorrente semanal.

`Projeto viagem: passagem, hotel, seguro`

→ Projeto com três tarefas.

## Qualidade

O branch inclui GitHub Actions executando testes unitários, lint e geração do APK debug.
