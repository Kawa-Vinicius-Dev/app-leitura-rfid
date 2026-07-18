# Inventário patrimonial com RFID

Aplicativo Android criado para apoiar inventários patrimoniais por meio da leitura de etiquetas RFID. O projeto organiza o fluxo de importação dos dados, seleção de loja e setor, leitura dos itens e exportação do resultado.

> Projeto em desenvolvimento. A integração completa de leitura depende de equipamento RFID compatível e da biblioteca fornecida pelo fabricante.

## Problema

Inventários manuais podem gerar retrabalho, demora na conferência e dificuldade para identificar itens registrados em locais diferentes do esperado. O aplicativo centraliza as etapas do inventário e fornece retorno visual durante a leitura.

## Funcionalidades implementadas

- Login e cadastro local de usuários
- Perfis de acesso para CEO, administrador e membro
- Importação de planilha de itens
- Importação e associação de lojas e setores
- Pesquisa de lojas e setores
- Leitura de etiquetas RFID em dispositivo compatível
- Validação de itens por loja e setor
- Identificação de divergências
- Ajuste da potência de leitura
- Exportação do resultado do inventário
- Feedback visual durante as etapas do fluxo

## Fluxo principal

~~~text
Login -> Importação dos dados -> Loja -> Setor -> Leitura RFID -> Conferência -> Exportação
~~~

## Tecnologias utilizadas

- Java 11
- Android SDK
- AndroidX
- Material Components
- SQLite
- Gradle
- Integração com biblioteca de leitor RFID

## Organização do código

~~~text
app/src/main/java/com/rktec/rfidapp/
├── data/          # acesso e persistência de dados
├── exportacao/    # geração dos arquivos de saída
├── importacao/    # leitura dos arquivos de entrada
├── model/         # modelos da aplicação
├── rfid/          # comunicação com o leitor
├── ui/            # activities e adapters
└── util/          # classes auxiliares
~~~

## Requisitos

- Android Studio
- JDK 11
- Android SDK 35
- Dispositivo Android 7.0 (API 24) ou superior
- Leitor RFID e biblioteca compatíveis para utilizar a leitura real

## Como executar

1. Clone o repositório:

~~~bash
git clone https://github.com/Kawa-Vinicius-Dev/app-leitura-rfid.git
~~~

2. Abra a pasta do projeto no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Confirme a disponibilidade das bibliotecas do equipamento RFID em `app/libs`.
5. Execute em um dispositivo compatível ou gere o APK de desenvolvimento:

~~~bash
./gradlew assembleDebug
~~~

No Windows, utilize `gradlew.bat assembleDebug`.

## Limitações atuais

- A leitura real depende de hardware e SDK específicos.
- Os usuários e permissões são armazenados localmente.
- O projeto ainda não possui uma suíte completa de testes automatizados.
- O fluxo de importação depende do formato esperado dos arquivos.
- A documentação do formato das planilhas ainda precisa ser detalhada.

## Próximos passos

- Documentar o modelo dos arquivos de entrada
- Adicionar testes para importação, validação e mapeamento
- Melhorar o tratamento de erros do dispositivo RFID
- Criar imagens demonstrativas sem expor dados reais
- Revisar a persistência e as permissões para cenários com múltiplos dispositivos

## Status

Em desenvolvimento e sujeito a ajustes conforme os testes com o equipamento.
