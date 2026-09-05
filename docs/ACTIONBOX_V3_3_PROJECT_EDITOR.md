# ActionBox V3.3 — Project Editor

Branch: `feature/actionbox-v3-3-project-editor`

## Objetivo

Transformar a tela de detalhes de Projetos em uma experiência também editável, preservando o fluxo de conclusão já existente e sem alterar o schema do banco.

## Funcionalidades

- Editar título do projeto.
- Editar descrição do projeto.
- Renomear tarefas existentes.
- Adicionar novas tarefas.
- Remover tarefas do projeto.
- Manter o check de conclusão das tarefas no modo de visualização.
- Reabrir automaticamente um projeto concluído quando uma nova tarefa é adicionada.
- Cancelar edição sem persistir alterações.
- Confirmação ao sair com alterações não salvas.
- Excluir o projeto com confirmação, removendo também as tarefas vinculadas.
- Atualização reativa da tela via Room/Flow depois de salvar.

## Persistência

Foi adicionado `@Update` para `ProjectEntity` no `ActionDao` e o método correspondente no `ActionRepository`.

As tarefas continuam sendo `ActionEntity` do tipo `TASK` vinculadas por `projectId`. Não houve mudança de schema nem necessidade de migration.

## Regras

- O título do projeto não pode ficar vazio.
- Tarefas existentes não podem ser salvas com título vazio.
- Novas tarefas vazias são ignoradas.
- Renomear uma tarefa preserva seu estado de conclusão.
- Adicionar tarefa a projeto concluído reabre o projeto.
- Excluir projeto remove todas as ações vinculadas a ele.

## Versão

- versionCode: 6
- versionName: 3.3.0

## Validação sugerida

1. Abrir Organizar > Projetos.
2. Abrir um projeto existente.
3. Tocar em Editar.
4. Alterar título e descrição.
5. Renomear uma tarefa.
6. Adicionar duas tarefas.
7. Remover uma tarefa existente.
8. Salvar e confirmar que o card do projeto e o detalhe foram atualizados.
9. Entrar em edição, alterar algo e voltar; confirmar o diálogo de descarte.
10. Finalizar o projeto, editar novamente e adicionar uma tarefa; confirmar que o projeto foi reaberto.
11. Testar exclusão do projeto e confirmar retorno para Organizar.
