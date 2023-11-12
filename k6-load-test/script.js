/*
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * OpenAPI spec version: v0
 *
 * NOTE: This class is auto generated by OpenAPI Generator.
 * https://github.com/OpenAPITools/openapi-generator
 *
 * OpenAPI generator version: 6.5.0
 */


import http from "k6/http";
import {check, group, sleep} from "k6";

export const options = {
    vus: 200,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'], // http errors should be less than 1%
    }
};

const BASE_URL = "http://localhost:8080";
// Sleep duration between successive requests.
// You might want to edit the value of this variable or remove calls to the sleep function on the script.
const SLEEP_DURATION = 1;

// Global variables should be initialized.

function create(entity, body) {
    let url = BASE_URL + `/speedy/v1/${entity}`;
    let params = {headers: {"Content-Type": "application/json", "Accept": "application/json"}};

    let response = http.post(url, JSON.stringify(body), params);

    const checkObj = {};
    checkObj[`${entity} successful creation.`] = (r) => r.status === 200

    check(response, checkObj);
    sleep(SLEEP_DURATION);
    return response.json().payload[0].id;
}

function findAll(entity) {
    let url = BASE_URL + `/speedy/v1/${entity}`;
    let response = http.get(url);
    const checkObj = {};
    checkObj[`${entity} successful fetch.`] = (r) => r.status === 200

    check(response, checkObj);
}

function findMany(entity, query) {
    let url = BASE_URL + `/speedy/v1/${entity}${query}`;
    let response = http.get(url);
    const checkObj = {};
    checkObj[`${entity} successful filter fetch.`] = (r) => r.status === 200

    check(response, checkObj);
}

function findOne(entity, id) {
    let url = BASE_URL + `/speedy/v1/${entity}(id='${id}')`;
    let response = http.get(url);


    const checkObj = {};
    checkObj[`${entity} successful single fetch.`] = (r) => r.status === 200

    check(response, checkObj);
    sleep(SLEEP_DURATION);
}

function update(entity, id, body) {
    let url = BASE_URL + `/speedy/v1/${entity}(id='${id}')`;

    let params = {headers: {"Content-Type": "application/json", "Accept": "application/json"}};
    let response = http.put(url, JSON.stringify(body), params);

    const checkObj = {};
    checkObj[`${entity} successful update.`] = (r) => r.status === 200

    check(response, checkObj);
}

function deleteById(entity, id) {
    let url = BASE_URL + `/speedy/v1/${entity}`;
    let params = {headers: {"Content-Type": "application/json", "Accept": "application/json"}};
    const body = [
        {
            id: id
        }
    ];
    let response = http.del(url, JSON.stringify(body), params);

    const checkObj = {};
    checkObj[`${entity} successful deletion.`] = (r) => r.status === 200

    check(response, checkObj);
}

export default function () {
    group("Procurement", () => {

        let data = {
            id: null
        };
        const entity = "Procurement";
        // Request No. 1: BulkCreateProcurement
        {
            let body = [
                {
                    "createdAt": "2023-05-13T09:42:03.397Z",
                    "supplier": {
                        "id": "1"
                    },
                    "dueAmount": Math.random() * 1000,
                    "product": {
                        "id": "1"
                    },
                    "createdBy": "K6Client",
                    "amount": Math.random() * 1000,
                    "purchaseDate": "2023-05-13T09:42:03.397Z"
                }
            ];
            data.id = create(entity, body);
        }

        // Request No.2: GetSomeProcurement
        findAll(entity);

        // Request No.3: GetSomeProcurement
        findMany(entity, "(amount> 0)");

        // Request No. 1: GetProcurement
        findOne(entity, data.id);

        // Request No. 2: UpdateProcurement
        {
            let body = {
                "supplier": {
                    "id": "3"
                },
                "dueAmount": Math.random() * 1000,
                "modifiedBy": "k6Client",
                "modifiedAt": "2023-05-13T09:42:03.397Z",
                "product": {
                    "id": "2"
                },
                "amount": Math.random() * 1000
            };
            update(entity, data.id, body);
        }


        // Request No. 3: BulkDeleteProcurement
        deleteById(entity, data.id);

    });

    group("Category", () => {

        let data = {
            id: null
        };
        const entity = "Category";
        // Request No. 1: BulkCreateProcurement
        {
            let body = [
                {
                    "name": "load test category " + Math.random(),
                }
            ];
            data.id = create(entity, body);
        }

        // Request No.2: GetSomeProcurement
        findAll(entity);

        // Request No.3: GetSomeProcurement
        findMany(entity, "");

        // Request No. 1: GetProcurement
        findOne(entity, data.id);

        // Request No. 2: UpdateProcurement
        {
            let body = {
                "name": "load test category " + Math.random(),
            };
            update(entity, data.id, body);
        }


        // Request No. 3: BulkDeleteProcurement
        deleteById(entity, data.id);

    });

    group("Product", () => {
        let data = {
            id: null
        };
        const entity = "Product";
        // Request No. 1: BulkCreateProcurement
        {
            let body = [
                {
                    "description": "load test product dest",
                    "name": "load test product " + Math.random(),
                    "category": {
                        "id": "1"
                    }
                }
            ];
            data.id = create(entity, body);
        }
        // Request No.2: GetSomeProcurement
        findAll(entity);
        // Request No.3: GetSomeProcurement
        findMany(entity, "");
        // Request No. 1: GetProcurement
        findOne(entity, data.id);
        // Request No. 2: UpdateProcurement
        {
            let body = {
                "description": "load test product desc update",
                "name": "load test product update " + Math.random(),
            };
            update(entity, data.id, body);
        }
        // Request No. 3: BulkDeleteProcurement
        deleteById(entity, data.id);
    });


}
