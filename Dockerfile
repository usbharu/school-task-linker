# ベースイメージとして、軽量なJava 17環境を使用
FROM eclipse-temurin:21-jre-alpine

# アプリケーションが使用するポート番号を定義
ARG APP_PORT=8080
EXPOSE ${APP_PORT}

# アプリケーションの実行ディレクトリを作成
WORKDIR /app

# ビルドされたfat JARをコンテナにコピー
# 'build/libs/' ディレクトリにある '-all.jar' で終わるファイルを app.jar としてコピー
COPY build/libs/*-all.jar app.jar

# コンテナ起動時にアプリケーションを実行
# Ktorの設定ファイルを外部から指定できるようにし、ポート番号を環境変数で上書き
ENTRYPOINT ["java", "-server", "-XX:+UseG1GC", "-jar", "app.jar", "-config=application.conf"]
