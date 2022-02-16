define(`GET_RESPONSE', `get:
      description: $1
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: schema/$2.json
        "500":
          $ref: "#/components/responses/trait_500"')dnl
dnl
define(`POST_RESPONSE', `post:
      description: $1
      requestBody:
        content:
          application/json:
            schema:
              $ref: schema/$2.json
      responses:
        "201":
          description: Created
          content:
            application/json:
              schema:
                $ref: schema/$3.json
        "400":
          $ref: "#/components/responses/trait_400"
        "500":
          $ref: "#/components/responses/trait_500"')dnl
dnl
define(`DELETE_RESPONSE', `delete:
      description: $1
      responses:
        "204":
          description: No content
        "404":
          $ref: "#/components/responses/trait_404")
        "500":
          $ref: "#/components/responses/trait_500"')dnl
dnl
define(`PUT_RESPONSE', `put:
      description: $1
      requestBody:
        content:
          application/json:
            schema:
              $ref: schema/$2.json
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: schema/$3.json
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
    GET_RESPONSE(`Get brief harvest job definitions', `harvestables')
    POST_RESPONSE(`Create new harvest job definition', `harvestableBrief', `harvestableBrief')
    DELETE_RESPONSE(`Delete all harvest job definitions')
  /harvester-admin/harvestables/{id}:
    GET_RESPONSE(`Get detailed harvest job definition', `harvestableBrief')
    PUT_RESPONSE(`Update a harvest job definition', `harvestableBrief', `harvestableBrief')
    DELETE_RESPONSE(`Delete a harvest job definition')
  /harvester-admin/jobs/run:
    PUT_RESPONSE(`Starts a harvest job immediately if possible', `runJob', `jobStarted')
  /harvester-admin/jobs/stop:
    POST_RESPONSE(`Stops a harvest job', `stopJob', `jobStopped')
  /harvester-admin/storages:
    GET_RESPONSE(`Get brief storage definition records', `storages')
    POST_RESPONSE(`Create new storage definition', `storageBrief', `storageBrief')
    DELETE_RESPONSE(`Delete all storage definitions')
  /harvester-admin/storages/{id}:
    GET_RESPONSE(`Get detailed storage definition record', `storageBrief')
    PUT_RESPONSE(`Update a storage definition', `storageBrief', `storageBrief')
    DELETE_RESPONSE(`Delete a storage definition')
  /harvester-admin/transformations:
    GET_RESPONSE(`Get brief transformation definition records', `transformations')
    POST_RESPONSE(`Create new transformation definition', `transformationBrief', `transformationBrief')
    DELETE_RESPONSE(`Delete all transformation definitions')
  /harvester-admin/transformations/{id}:
    GET_RESPONSE(`Get detailed transformation definition record', `transformationBrief')
    PUT_RESPONSE(`Update a transformation definition', `transformationBrief', `transformationBrief')
    DELETE_RESPONSE(`Delete a transformation definition')
  /harvester-admin/steps:
    GET_RESPONSE(`Get brief transformation step definition records', `steps')
    POST_RESPONSE(`Create new transformation step definition', `stepBrief', `stepBrief')
    DELETE_RESPONSE(`Delete all transformation step definitions')
  /harvester-admin/steps/{id}:
    GET_RESPONSE(`Get detailed transformation set definition record', `stepBrief')
    PUT_RESPONSE(`Update a transformation step definition', `stepBrief', `stepBrief')
    DELETE_RESPONSE(`Delete a transformation step definition')
  /harvester-admin/steps/{id}/script:
    GET_RESPONSE(`Get transformation step script', `script')
    PUT_RESPONSE(`Update a transformation step script', `script', `script')
  /harvester-admin/tsas:
    GET_RESPONSE(`Get transformation step association records', `transformationStepAssociations')
    POST_RESPONSE(`Create new transformation step association', `transformationStepAssociation', `transformationStepAssociation')
    DELETE_RESPONSE(`Delete all transformation step associations')
  /harvester-admin/tsas/{id}:
    GET_RESPONSE(`Get transformation step association record', `transformationStepAssociation')
    PUT_RESPONSE(`Update a transformation step association', `transformationStepAssociation', `transformationStepAssociation')
    DELETE_RESPONSE(`Delete a transformation step association')
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
