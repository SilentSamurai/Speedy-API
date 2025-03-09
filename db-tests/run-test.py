import subprocess
import os

def sh(command: str):
    sh_list = list(filter(lambda x: len(x) > 0, command.split()))
    print("running command ", sh_list)
    job = subprocess.run(sh_list, shell=True)
    if job.returncode != 0:
        raise Exception(f"{command} failed")


def main():
    sh("docker run -d \
           --name speedy-test-postgres-db \
           -e POSTGRES_DB=testdb \
           -e POSTGRES_USER=user \
           -e POSTGRES_PASSWORD=password \
           -p 5432:5432 \
           postgres:latest")

    os.environ['DATABASE_URL'] = "jdbc:postgresql://localhost:5432/testdb"
    os.environ['DATABASE_USERNAME'] = "user"
    os.environ['DATABASE_PASSWORD'] = "password"
    os.environ['SPRING_PROFILES_ACTIVE'] = "postgres"

    # sh("DATABASE_URL=")
    try:
        os.chdir("../speedy-test-app")
        # sh("mvn test -Dtest=TheFirstUnitTest")
        sh("mvn test ")
    except:
        print("test failed")
    finally:
        sh("docker stop speedy-test-postgres-db")
        sh("docker rm speedy-test-postgres-db")


if __name__ == "__main__":
    main()
