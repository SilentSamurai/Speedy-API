import argparse
import os
import subprocess


def sh(command: str):
    sh_list = list(filter(lambda x: len(x) > 0, command.split()))
    print("running command ", sh_list)
    job = subprocess.run(sh_list, shell=True)
    if job.returncode != 0:
        raise Exception(f"{command} failed")


def run_for_db(db_name: str, test_pattern: str):
    db_conf = suits[db_name]
    sh(db_conf["START_CONTAINER_CMD"])

    os.environ['DATABASE_URL'] = db_conf["DATABASE_URL"]
    os.environ['DATABASE_USERNAME'] = db_conf["DATABASE_USERNAME"]
    os.environ['DATABASE_PASSWORD'] = db_conf["DATABASE_PASSWORD"]
    os.environ['SPRING_PROFILES_ACTIVE'] = db_conf["SPRING_PROFILES_ACTIVE"]

    try:
        os.chdir("../speedy-test-app")
        if test_pattern:
            sh(f"mvn test -Dtest={test_pattern}")
        else:
            sh("mvn test")
    except:
        print("test failed")
    finally:
        container_name = db_conf["CONTAINER_NAME"]
        sh(f"docker stop {container_name}")
        sh(f"docker rm {container_name}")

    return True


def main():
    parser = argparse.ArgumentParser(
        description="Run speedy-test-app tests against PostgreSQL and MySQL Docker containers"
    )
    parser.add_argument(
        "--tests", "-t",
        default=None,
        help="Surefire test pattern (e.g. 'PkUuidTestTest', 'SpeedyGetTest,SpeedyPostTest'). "
             "Omit to run the full suite."
    )
    args = parser.parse_args()

    os.chdir("../")
    sh("mvn install -DskipTests")
    os.chdir("./db-tests")

    for db in ["MYSQL", "POSTGRES"]:
        print(f"==============================> [Starting {db}] <================================")
        run_for_db(db, args.tests)
        print(f"==============================> [Ending {db}] <====================================")


suits = {
    "POSTGRES": {
        "CONTAINER_NAME": "speedy-test-postgres-db",
        "START_CONTAINER_CMD": "docker run -d \
           --name speedy-test-postgres-db \
           -e POSTGRES_DB=testdb \
           -e POSTGRES_USER=user \
           -e POSTGRES_PASSWORD=password \
           -p 5432:5432 \
           postgres:latest",

        "DATABASE_URL": "jdbc:postgresql://localhost:5432/testdb",
        "DATABASE_USERNAME": "user",
        "DATABASE_PASSWORD": "password",
        "SPRING_PROFILES_ACTIVE": "postgres"
    },
    "MYSQL": {
        "CONTAINER_NAME": "speedy-test-mysql-db",
        "START_CONTAINER_CMD": "docker run -d \
           --name speedy-test-mysql-db \
           -e MYSQL_ROOT_PASSWORD=rootpassword \
           -e MYSQL_DATABASE=mydatabase \
           -e MYSQL_USER=user \
           -e MYSQL_PASSWORD=password \
           -p 3306:3306 \
           mysql:latest",

        "DATABASE_URL": "jdbc:mysql://localhost:3306/mydatabase",
        "DATABASE_USERNAME": "user",
        "DATABASE_PASSWORD": "password",
        "SPRING_PROFILES_ACTIVE": "mysql"
    }
}

if __name__ == "__main__":
    main()
