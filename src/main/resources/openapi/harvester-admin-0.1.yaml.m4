define(`GET_RESPONSE', `get:
      description: $1
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: $2
        "500":
          $ref: "#/components/responses/trait_500"')dnl
dnl
define(`PUT_RESPONSE', `put:
      description: $1
      requestBody:
        content:
          application/json:
            schema:
              $ref: $2
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: $3
        "400":
          $ref: "#/components/responses/trait_400"
        "404":
          $ref: "#/components/responses/trait_404"
        "500":
          $ref: "#/components/responses/trait_500"')dnl
dnl
openapi: 3.0.0
info:
  title: Harvester Admin API
  version: v0.1
paths:
  /harvester-admin/harvestables:
    GET_RESPONSE(`Get brief harvest job definitions', `schema/harvestables.json')
  /harvester-admin/jobs/run:
    PUT_RESPONSE(`Starts a harvest job immediately if possible', `schema/runJob.json', `schema/jobStarted.json')
  /harvester-admin/storages:
    GET_RESPONSE(`Get brief storage definition records', `schema/storages.json')
  /harvester-admin/transformations:
    GET_RESPONSE(`Get brief transformation definition records', `schema/transformations.json')
  /harvester-admin/steps:
    GET_RESPONSE(`Get brief transformation step definition records', `schema/steps.json')
  /harvester-admin/tsas:
    GET_RESPONSE(`Get transformation step association records', `schema/steps.json')
components:
  responses:
    trait_400:
      description: Bad request
      content:
        text/plain:
          schema:
            type: string
            example: Invalid JSON in request
    trait_404:
      description: Not Found
      content:
        text/plain:
          schema:
            type: string
            example: 1234 not found
    trait_422:
      description: Unprocessable entity
      content:
        text/plain:
          schema:
            type: string
            example: Record already exists
    trait_500:
      description: Internal error
      content:
        text/plain:
          schema:
            type: string
            example: Internal server error, contact administrator
